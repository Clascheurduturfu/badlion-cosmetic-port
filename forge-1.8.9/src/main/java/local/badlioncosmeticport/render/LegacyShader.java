package local.badlioncosmeticport.render;

import local.badlioncosmeticport.MeshCosmetics;
import org.lwjgl.opengl.GL20;
import net.minecraft.client.renderer.OpenGlHelper;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Compatibility GLSL adapter for the two recovered procedural material effects. */
public final class LegacyShader {
    private static int program;
    private static boolean failed;
    private static final long epoch=System.nanoTime();
    public static long draws;
    private LegacyShader() {}
    private static String source(String name) throws Exception {
        try (InputStream in=LegacyShader.class.getResourceAsStream("/assets/badlioncosmeticport/shaders/"+name)) {
            if (in==null) throw new java.io.IOException("Missing shader "+name);
            ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[8192];int n;
            while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private static int compile(int kind,String name) throws Exception {
        int shader=GL20.glCreateShader(kind);
        GL20.glShaderSource(shader,source(name));GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0) {
            String error=GL20.glGetShaderInfoLog(shader,8192);GL20.glDeleteShader(shader);throw new java.io.IOException(error);
        }
        return shader;
    }
    public static int begin(MeshCosmetics.Pack pack) {
        int previous=org.lwjgl.opengl.GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean shield=pack.type==MeshCosmetics.Category.SHIELD && pack.id==250;
        boolean bandana=pack.type==MeshCosmetics.Category.BANDANA && pack.id==345;
        if ((!shield&&!bandana)||failed) return previous;
        try {
            if(program==0) {
                int vertex=compile(GL20.GL_VERTEX_SHADER,"cosmetic.vsh"), fragment=compile(GL20.GL_FRAGMENT_SHADER,"cosmetic.fsh");
                program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
                GL20.glDeleteShader(vertex);GL20.glDeleteShader(fragment);
                if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0) throw new java.io.IOException(GL20.glGetProgramInfoLog(program,8192));
            }
            GL20.glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"Sampler0"),0);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"Lightmap"),1);
            GL20.glUniform2f(GL20.glGetUniformLocation(program,"LightUV"),(OpenGlHelper.lastBrightnessX+8)/256f,(OpenGlHelper.lastBrightnessY+8)/256f);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"shieldFX"),shield?1:0);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"bandanaFX"),bandana?1:0);
            GL20.glUniform1f(GL20.glGetUniformLocation(program,"iTime"),(float)((System.nanoTime()-epoch)/1e9));draws++;
        } catch(Exception error) {
            failed=true; GL20.glUseProgram(previous);
            org.apache.logging.log4j.LogManager.getLogger("BadlionCosmeticPort").error("Legacy cosmetic shader failed",error);
        }
        return previous;
    }
    public static void end(int previous) { GL20.glUseProgram(previous); }
}
