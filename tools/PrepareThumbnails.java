import com.google.gson.*;
import local.badlioncosmeticport.model.*;
import local.badlioncosmeticport.render.ObjGeometry;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;

/** Offline, bounded-size product thumbnails from the actual exported geometry and first HD frame. */
public final class PrepareThumbnails {
    private record V(float x,float y,float z,float u,float v) {}
    private record T(V a,V b,V c) {}
    private static final int SIZE=128;
    private static final Map<String,List<T>> GEOMETRY=new HashMap<>();
    private static V v(Md5Model.SkinnedVertex p) { return new V(p.x(),p.y(),p.z(),p.u(),p.v()); }
    private static V v(ObjGeometry.Vertex p) { return new V(p.x(),p.y(),p.z(),p.u(),p.v()); }
    private static List<T> geometry(Path root,String model) throws Exception {
        if(GEOMETRY.containsKey(model))return GEOMETRY.get(model);
        List<T> result=new ArrayList<>();
        if(model.endsWith(".md5mesh")) {
            var mesh=Md5Model.read(root.resolve(model));
            var pose=mesh.restPose();
            Path flap=root.resolve(model).resolveSibling("flapping.md5anim");
            if(model.contains("/wing/")&&Files.isRegularFile(flap)) pose=Md5Animation.read(flap).sampleFrame(9,false);
            for(var part:mesh.skin(pose))for(var t:part.triangles())
                result.add(new T(v(part.vertices().get(t.a())),v(part.vertices().get(t.b())),v(part.vertices().get(t.c()))));
        } else for(var t:ObjGeometry.read(root.resolve(model)).triangles())result.add(new T(v(t.a()),v(t.b()),v(t.c())));
        GEOMETRY.put(model,result);return result;
    }
    private static List<T> ribbon() {
        var strip=LegacyCloak.strip(LegacyCloak.Shape.DYNAMIC,new LegacyCloak.Motion(0,0,0,0,0,0,0,.35f),0);
        List<T> result=new ArrayList<>();
        for(int i=2;i<strip.size();i++){
            var a=strip.get(i-2);var b=strip.get(i-1);var c=strip.get(i);
            result.add(new T(new V(a.x(),a.y(),a.z(),a.u(),a.v()),new V(b.x(),b.y(),b.z(),b.u(),b.v()),new V(c.x(),c.y(),c.z(),c.u(),c.v())));
        }return result;
    }
    private static V transform(V p,Matrix4f m,boolean flip) {
        Vector3f point=m.transformPosition(new Vector3f(p.x,p.y,p.z));
        return new V(point.x,point.y,point.z,p.u,flip?1-p.v:p.v);
    }
    private static float edge(V a,V b,float x,float y){return (x-a.x)*(b.y-a.y)-(y-a.y)*(b.x-a.x);}
    private static void render(List<T> input,Matrix4f matrix,boolean flip,boolean wings,BufferedImage texture,Path target)throws Exception {
        List<T> triangles=new ArrayList<>();
        for(var t:input){triangles.add(new T(transform(t.a,matrix,flip),transform(t.b,matrix,flip),transform(t.c,matrix,flip)));
            if(wings){var mirrored=new Matrix4f(matrix).scale(-1,1,1);triangles.add(new T(transform(t.a,mirrored,flip),transform(t.b,mirrored,flip),transform(t.c,mirrored,flip)));}}
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=-minX,maxY=maxX;
        for(var t:triangles)for(var p:new V[]{t.a,t.b,t.c}){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);}
        float scale=(SIZE-16)/Math.max(.0001f,Math.max(maxX-minX,maxY-minY)),cx=(minX+maxX)/2,cy=(minY+maxY)/2;
        BufferedImage output=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
        float[] depth=new float[SIZE*SIZE];Arrays.fill(depth,Float.NEGATIVE_INFINITY);
        for(var t:triangles){
            V[] p=new V[3];int j=0;for(var a:new V[]{t.a,t.b,t.c})p[j++]=new V((a.x-cx)*scale+SIZE/2f,SIZE/2f-(a.y-cy)*scale,a.z,a.u,a.v);
            float area=edge(p[0],p[1],p[2].x,p[2].y);if(Math.abs(area)<.0001)continue;
            int x0=Math.max(0,(int)Math.floor(Math.min(p[0].x,Math.min(p[1].x,p[2].x)))),x1=Math.min(SIZE-1,(int)Math.ceil(Math.max(p[0].x,Math.max(p[1].x,p[2].x))));
            int y0=Math.max(0,(int)Math.floor(Math.min(p[0].y,Math.min(p[1].y,p[2].y)))),y1=Math.min(SIZE-1,(int)Math.ceil(Math.max(p[0].y,Math.max(p[1].y,p[2].y))));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
                float a=edge(p[1],p[2],x+.5f,y+.5f)/area,b=edge(p[2],p[0],x+.5f,y+.5f)/area,c=1-a-b;
                if(a<-.0001||b<-.0001||c<-.0001)continue;
                float z=a*p[0].z+b*p[1].z+c*p[2].z;if(z<depth[y*SIZE+x])continue;
                float u=a*p[0].u+b*p[1].u+c*p[2].u,v=a*p[0].v+b*p[1].v+c*p[2].v;
                int rgba=texture.getRGB(Math.max(0,Math.min(texture.getWidth()-1,(int)(u*texture.getWidth()))),Math.max(0,Math.min(texture.getHeight()-1,(int)(v*texture.getHeight()))));
                if((rgba>>>24)<20)continue;
                output.setRGB(x,y,rgba);depth[y*SIZE+x]=z;
            }
        }
        Files.createDirectories(target.getParent());ImageIO.write(output,"PNG",target.toFile());
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var packs=JsonParser.parseString(Files.readString(root.resolve("catalog.json"))).getAsJsonObject().getAsJsonArray("packs");
        int generated=0,failed=0;
        for(var entry:packs){var pack=entry.getAsJsonObject();String type=pack.get("type").getAsString();int id=pack.get("id").getAsInt();
            if(args.length>1&&!type.equals(args[1]))continue;
            Path target=root.resolve("thumbnails/"+type+"/"+id+".png");
            try{
                var catalog=pack.getAsJsonObject("catalog");
                boolean procedural=type.equals("CAPE")&&(!catalog.has("modelName")||catalog.get("modelName").isJsonNull()||catalog.get("modelName").getAsString().isBlank());
                Matrix4f orientation=new Matrix4f().rotateX(.15f).rotateY(-.25f);
                String model=pack.get("model").getAsString();
                if(procedural)orientation.rotateY(-(float)Math.PI/2);
                else if(model.endsWith(".md5mesh")&&!type.equals("WINGS"))orientation.rotateX(-(float)Math.PI/2);
                var metadata=pack.getAsJsonObject("modelMetadata");
                if(type.equals("SHIELD")&&metadata!=null&&metadata.has("shieldModelData")){
                    var shield=metadata.getAsJsonObject("shieldModelData");
                    if(shield.has("inverted")&&shield.get("inverted").getAsBoolean())orientation.rotateY((float)Math.PI);
                }
                boolean flip=metadata!=null&&metadata.has("flipUVs")&&metadata.get("flipUVs").getAsBoolean();
                BufferedImage texture=ImageIO.read(root.resolve(pack.getAsJsonArray("frames").get(0).getAsString()).toFile());
                render(procedural?ribbon():geometry(root,model),orientation,flip,type.equals("WINGS"),texture,target);generated++;
            }catch(Exception failure){failed++;System.err.println(type+"/"+id+": "+failure);}
            if((generated+failed)%300==0)System.out.println("Thumbnails "+(generated+failed)+"/"+packs.size());
        }
        if(args.length==1)Files.writeString(root.resolve("thumbnails/manifest.json"),"{\"generated\":"+generated+",\"failed\":"+failed+",\"size\":128,\"source\":\"original models and first HD texture frame\"}");
        if(failed>0)throw new IllegalStateException("Thumbnail failures: "+failed);
        System.out.println("Generated "+generated+" model thumbnails");
    }
}
