#version 450 core

#define FRAG_COLOR	0
#define DIFFUSE		0

layout(binding = DIFFUSE) uniform sampler2D Diffuse;

in vert
{
	//vec2 Texcoord;
        //sample vec2 Texcoord;
        vec2 Texcoord;
} Vert;

layout(location = FRAG_COLOR, index = 0) out vec4 Color;

void main()
{
	//Color = texture(Diffuse, interpolateAtSample(Vert.Texcoord, gl_SampleID));
	//Color = texture(Diffuse, Vert.Texcoord);
        Color = vec4(1, 0.5, 0, 1);
}
