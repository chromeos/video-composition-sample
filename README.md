# Video Composition Sample

## This is not an officially supported Google product

## About
The aim of this project is to:
 * Demonstrate a possible approach to provide basic video composition operations on Android and
 Chrome OS including: multiplexing 4 video streams, rotation/transparency/colour filter effects,
 with export to a mp4 file.
 * Provide a base for testing media decoding/muxing/encoding performance with various file sizes and
 types

The video composition sample decodes 4 video streams, applies optional effects to each one -
rotation, transparency, scaling, colour filters, speed up, slow down - combines them in a realtime
preview. An export option for the composition is also provided.

<img alt="Screenshot of Video Composition Sample" src="https://github.com/chromeos/video-composition-sample/blob/main/docs/VideoCompositionSampleScreenshot.jpg" width="200" />

### Notes/Caveats
 * Effects are configured in MediaCompositionConfig.kt by setting a list of TrackEffects with start
 and duration and effect parameters.
 * Currently video clips are assumed to have a framerate set. Clips without a framerate will cause
 a crash on decode.
 * The media files included with this project are large (~600mb in total). To checkout this repo
 please use: [git-lfs](https://git-lfs.github.com/). Install git-lfs then run `git lfs install`.

## LICENSE
***

Copyright 2021 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
