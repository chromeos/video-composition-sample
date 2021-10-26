/*
 * Copyright (c) 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES u_Texture;
uniform float opacity;
uniform float sepia;
uniform float sepiaCenterPosition;

varying vec2 vTexCoordinate;

void main() {

	vec4 textureColor = texture2D(u_Texture, vTexCoordinate);
    textureColor.a = opacity;

	mat4 sepiaColorMatrix = mat4(
		0.3588, 0.7044, 0.1368, 0.0,
        0.2990, 0.5870, 0.1140, 0.0,
        0.2392, 0.4696, 0.0912, 0.0,
        0.0,    0.0,    0.0,    0.0
    );
    vec4 sepiaColor = textureColor * sepiaColorMatrix;
    sepiaColor.a = opacity;

	if (vTexCoordinate.x < sepiaCenterPosition - 0.25 || vTexCoordinate.x > sepiaCenterPosition + 0.25 ) {
		gl_FragColor = (sepia * sepiaColor) + ((1.0 - sepia) * textureColor);
	} else {
		gl_FragColor = sepiaColor;
	}

}