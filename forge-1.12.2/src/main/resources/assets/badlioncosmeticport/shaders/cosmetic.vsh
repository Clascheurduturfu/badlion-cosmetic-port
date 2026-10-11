#version 120
varying vec2 texCoord0;
varying vec4 vertexColor;
varying float vertexDistance;
void main() {
    vec4 eye=gl_ModelViewMatrix*gl_Vertex;
    gl_Position=gl_ProjectionMatrix*eye;
    texCoord0=gl_MultiTexCoord0.xy;
    vertexColor=gl_Color;
    vertexDistance=length(eye.xyz);
}
