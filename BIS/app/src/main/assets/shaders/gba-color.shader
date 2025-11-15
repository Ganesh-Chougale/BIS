<?xml version="1.0" encoding="UTF-8"?>

<shader language="GLSL">
  <fragment filter="nearest" output_width="100%" output_height="100%"><![CDATA[
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform sampler2D rubyTexture;
varying vec2 vTexCoord;

#define darken_screen 0.75
#define target_gamma 2.2
#define display_gamma 2.2
#define sat 1.0
#define lum 0.94
#define contrast 1.0
#define blr 0.0
#define blg 0.0
#define blb 0.0
#define r 0.82
#define g 0.665
#define b 0.73
#define rg 0.125
#define rb 0.195
#define gr 0.24
#define gb 0.075
#define br -0.06
#define bg 0.21

void main() {
	vec4 screen = pow(texture2D(rubyTexture, vTexCoord), vec4(target_gamma + darken_screen)).rgba;
	vec4 avglum = vec4(0.5);
	screen = mix(screen, avglum, (1.0 - contrast));

	//                  r   g     b     black
	mat4 color = mat4(	r,  rg,   rb,  0.0,  //red channel
						gr,  g,   gb,  0.0,  //green channel
						br,  bg,  b,   0.0,  //blue channel
						blr, blg, blb, 0.0); //alpha channel; these numbers do nothing for our purposes.

	mat4 adjust = mat4(
		(1.0 - sat) * 0.3086 + sat, (1.0 - sat) * 0.3086, (1.0 - sat) * 0.3086, 1.0,
		(1.0 - sat) * 0.6094, (1.0 - sat) * 0.6094 + sat, (1.0 - sat) * 0.6094, 1.0,
		(1.0 - sat) * 0.0820, (1.0 - sat) * 0.0820, (1.0 - sat) * 0.0820 + sat, 1.0,
		0.0, 0.0, 0.0, 1.0);

	color *= adjust;
	screen = clamp(screen * lum, 0.0, 1.0);
	screen = color * screen;
	gl_FragColor = pow(screen, vec4(1.0 / display_gamma));
}
]]></fragment>
</shader>
