# Official stb_image_resize v0.97

Unmodified upstream files from `nothings/stb` commit
`5736b15f7ea0ffb08dd38af21067c314d6a3aae9`:

* `stb_image_resize.h`: SHA-256
  `6a0e75adbabb48df9031c2e39ccd97437bb226fe31e7d4a01c7bf70a18d32ec6`
* `LICENSE`: SHA-256
  `bebfe904b14301657e4e5d655c811d51fd31b97c455b9cc2d8600d6bac6cff63`

Repository: https://github.com/nothings/stb

The header is byte-for-byte identical to LWJGL 3.3.3's
`modules/lwjgl/stb/src/main/c/stb_image_resize.h` at its release commit
`f0ec1437a6149dae00eaa27c47268e5e4efb129f`.
The embedded dual MIT/public-domain license and upstream standalone license are
retained. No algorithm edits, alternate filters, alpha reinterpretation, or STB2
implementation are introduced. The build compiles the header through a
directives-only C translation unit and calls it from ordinary Rust JNI source.
