#version 120
uniform sampler2D Sampler0; uniform sampler2D Lightmap; uniform vec2 LightUV; varying vec2 texCoord0; varying vec4 vertexColor; varying float vertexDistance;
vec4 linear_fog(vec4 color,float distance,float start,float finish,vec4 fog) { return mix(color,fog,clamp((distance-start)/max(0.001,finish-start),0.0,1.0)); }
#define FogStart gl_Fog.start
#define FogEnd gl_Fog.end
#define FogColor gl_Fog.color
uniform float iTime;
uniform bool shieldFX;

#define PI 3.1415926538
#define blur 0.99
#define timeMul 2.0
#define boxCount 10.0
#define height 1.0
#define verticalStretch 20.0

uniform bool bandanaFX;

#define bandanaRounding 0.05
#define bandanaBlur 0.95
#define bandanaTimeMul 2.0
#define bandanaHeight 1.0
#define bandanaBoxCount 5.0
#define bandanaVerticalStretch 5.0

float atan2(vec2 vector) {
    float theta = atan(vector.y / vector.x);
    if (vector.x > 0.0) {
        return theta;
    }
    if (vector.y >= 0.0) {
        return theta + PI;
    }
     return theta - PI;
    
    return atan(vector.y / vector.x);
}

float sdfRoundedBox(vec2 uv, vec2 mid, vec2 dimensions, float rounding) {
    vec2 offset = abs(mid - uv);
    dimensions -= rounding * 2.0;
    vec2 dif = offset - (dimensions * 0.5);
    return length(max(dif, 0.0));
}

void main() {
    if (shieldFX) {
        vec2 uv = texCoord0;
        uv.y *= 0.5;
        vec2 mid = vec2(0.25, 0.25);
        vec2 vec = mid - uv;
        float theta = atan2(vec);
        float x = ((theta + PI) * 0.5) / PI;
        float y = (0.25 - length(vec)) * 4.0;
        float inverseX = abs(vec.x);
        
        if (y > 0.0) {
            float rounding = 0.1;
            vec2 uv2 = vec2(x,y);
            uv2 *= vec2(boxCount, verticalStretch);
            uv2.x = fract(uv2.x);
            if (uv2.y < height) {
                uv2.y = fract(uv2.y);
            }
            
            float time = fract(iTime * timeMul);
            
            float y = min(time * 2.0, 1.0) / 2.0;
            float x = max(time * 2.0 - 1.0, 0.0) / 2.0;
            
            float sdf1 = sdfRoundedBox(uv2, vec2(0.25 + x, 0.25 + y), vec2(0.5), rounding);
            float sdf2 = sdfRoundedBox(uv2, vec2(0.75 - x, 0.75 - y), vec2(0.5), rounding);
            
            if (sdf1 <= rounding) {
                gl_FragColor = vec4(vec3(1.0, 0.0, 0.0), 1.0) * (1.0 - smoothstep(rounding * blur, rounding, sdf1));
                gl_FragColor = linear_fog(gl_FragColor, vertexDistance, FogStart, FogEnd, FogColor);
                return;
            }
            if (sdf2 <= rounding) {
                gl_FragColor = vec4(vec3(1.0, 0.0, 0.0), 1.0) * (1.0 - smoothstep(rounding * blur, rounding, sdf2));
                gl_FragColor = linear_fog(gl_FragColor, vertexDistance, FogStart, FogEnd, FogColor);
                return;
            }
        }
    }
    if (bandanaFX) {
        vec2 uv = texCoord0;
        
        float dif = min(0.6, uv.y);
        float units = min(floor(dif / 0.15), 3.0);
        
        vec2 mid = vec2(0.5, 0.075 + units * 0.15);
        vec2 dimensions = vec2(1.0 / 3.0, 0.15);
        float halfHeight = dimensions.y / 2.0;
        float bot = mid.y - halfHeight;
        float right = mid.x + dimensions.x / 2.0;
        
        float sdf = sdfRoundedBox(uv, mid, dimensions, 0.0);
        
        if (sdf <= 0.0) {
            float yDif = (halfHeight - abs(mid.y - uv.y)) / halfHeight;
            float xDif = (right - uv.x) / dimensions.x;
            
            vec2 uv2 = vec2(xDif, yDif);
            
            uv2 *= vec2(bandanaBoxCount, bandanaVerticalStretch);
            uv2.x = fract(uv2.x);
            if (uv2.y < bandanaHeight) {
                uv2.y = fract(uv2.y);
            }
            
            float time = fract(iTime * bandanaTimeMul);
            
            float y = min(time * 2.0, 1.0) / 2.0;
            float x = max(time * 2.0 - 1.0, 0.0) / 2.0;
            
            float sdf1 = sdfRoundedBox(uv2, vec2(0.25 + x, 0.25 + y), vec2(0.5), bandanaRounding);
            float sdf2 = sdfRoundedBox(uv2, vec2(0.75 - x, 0.75 - y), vec2(0.5), bandanaRounding);
            
            if (sdf1 <= bandanaRounding) {
                gl_FragColor = vec4(vec3(1.0, 0.0, 0.0), 1.0) * (1.0 - smoothstep(bandanaRounding * bandanaBlur, bandanaRounding, sdf1));
                gl_FragColor = linear_fog(gl_FragColor, vertexDistance, FogStart, FogEnd, FogColor);
                return;
            }
            if (sdf2 <= bandanaRounding) {
                gl_FragColor = vec4(vec3(1.0, 0.0, 0.0), 1.0) * (1.0 - smoothstep(bandanaRounding * bandanaBlur, bandanaRounding, sdf2));
                gl_FragColor = linear_fog(gl_FragColor, vertexDistance, FogStart, FogEnd, FogColor);
                return;
            }
        }
    }

    vec4 color = texture2D(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor;

    color *= texture2D(Lightmap,LightUV);
    gl_FragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

