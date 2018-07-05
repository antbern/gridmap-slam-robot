#shader vertex
#version 330 core


layout(location = 0) in vec4 position;
layout(location = 1) in vec4 color;

uniform mat4 u_projModelView;

varying vec4 v_Color;
void main(){
	// output the final vertex position
	gl_Position = u_projModelView * position;
		
	v_Color = vec4(color.xyz, 1.0);
};





#shader fragment

#version 330 core

layout(location = 0) out vec4 color;
//uniform vec4 u_Color;

varying vec4 v_Color;
void main(){
	color = v_Color;//vec4(1.0, 1.0, 0.0, 1.0);
};