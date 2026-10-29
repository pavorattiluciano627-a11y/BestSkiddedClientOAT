#!/usr/bin/env python3
"""Add one log line at each hop of the startup chain that fails silently.

Symptoms this diagnoses: the client loads without crashing, but the ClickGUI
key does nothing and no ``config/echo`` directory is ever created.

Chain, and why each hop can fail invisibly:

    Echo.onResourceManagerReload   creates uiRenderer / worldRenderer
    Echo.uiRenderer                stays null if the listener never fires
    ScreenManager.open             returns silently when uiRenderer is null
    FeatureConfig.save             only reached from the GUI or Minecraft.close

Idempotent: safe to run more than once.

    python3 tools/port/diag_startup.py [--root Echo/echo-clean-source-035]
"""

import argparse
import pathlib
import sys

EDITS = [
    (
        "src/main/java/dev/echo/client/Echo.java",
        "    public void onResourceManagerReload(ResourceManager resources) {\n"
        "        initializeReferencedClass(UiRenderer.class);",
        '    public void onResourceManagerReload(ResourceManager resources) {\n'
        '        LOGGER.info("[echo] reload listener fired: uiRenderer={} renderDevice={} "\n'
        '                + "featureManager={} featureConfig={}",\n'
        '                uiRenderer != null, renderDevice != null,\n'
        '                featureManager != null, featureConfig != null);\n'
        "        initializeReferencedClass(UiRenderer.class);",
    ),
    (
        "src/main/java/dev/echo/client/Echo.java",
        '        particleManager.initialize();\n'
        '        featureConfig.load("_autosave");\n'
        "        featureConfig.loading = false;\n"
        "        SystemInformation.send();",
        '        particleManager.initialize();\n'
        '        featureConfig.load("_autosave");\n'
        "        featureConfig.loading = false;\n"
        '        LOGGER.info("[echo] client runtime ready: uiRenderer={} worldRenderer={} handlers={}",\n'
        "                uiRenderer != null, worldRenderer != null,\n"
        "                handlerManager != null ? handlerManager.size() : -1);\n"
        "        SystemInformation.send();",
    ),
    (
        "src/main/java/dev/echo/client/screens/ScreenManager.java",
        "    public void open() {\n"
        "        if (Echo.eventsDisabled || Echo.uiRenderer == null) {\n"
        "            return;\n"
        "        }",
        "    public void open() {\n"
        "        if (Echo.eventsDisabled || Echo.uiRenderer == null) {\n"
        '            Echo.LOGGER.warn("[echo] ClickGUI open refused: eventsDisabled={} uiRenderer={} "\n'
        '                    + "(the renderer is built by the resource-reload listener; if this "\n'
        '                    + "repeats, that listener never ran)",\n'
        "                    Echo.eventsDisabled, Echo.uiRenderer != null);\n"
        "            return;\n"
        "        }",
    ),
    (
        "src/main/java/dev/echo/client/bootstrap/ClientBootstrap.java",
        "            Echo echo = new Echo();\n            echo.onInitialize();",
        "            Echo echo = new Echo();\n"
        "            echo.onInitialize();\n"
        '            Echo.LOGGER.info("[echo] bootstrap INITIALIZED: features={} registered={} "\n'
        '                    + "skipped={} handlers={} screenManager={}",\n'
        "                    allocated.size(), sorted.size(), skipped.size(),\n"
        "                    Echo.handlerManager != null ? Echo.handlerManager.size() : -1,\n"
        "                    Echo.screenManager != null);\n"
        "            for (SkippedFeature miss : skipped) {\n"
        '                Echo.LOGGER.error("[echo] feature not allocated: {} -> {}",\n'
        '                        miss.name(), miss.cause() == null ? "?" : miss.cause().toString());\n'
        "            }",
    ),
]

ACCESSOR = (
    "src/main/java/dev/echo/client/handlers/HandlerManager.java",
    "    /** Returns the rotation manager created with this handler manager. */",
    "    /** Number of initialized handlers; 0 means {@link #initialize()} never ran. */\n"
    "    public int size() {\n"
    "        return handlers.size();\n"
    "    }\n\n"
    "    /** Returns the rotation manager created with this handler manager. */",
)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    args = ap.parse_args()
    root = pathlib.Path(args.root)

    changed = 0
    for rel, old, new in EDITS + [ACCESSOR]:
        path = root / rel
        if not path.exists():
            print(f"missing {rel}", file=sys.stderr)
            return 1
        text = path.read_text()
        if new in text or (rel.endswith("HandlerManager.java") and "public int size()" in text):
            continue
        if old not in text:
            print(f"anchor not found in {rel}", file=sys.stderr)
            return 1
        path.write_text(text.replace(old, new, 1))
        changed += 1
        print(f"instrumented {rel.split('/')[-1]}")

    print(f"{changed} edits applied" if changed else "already instrumented")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
