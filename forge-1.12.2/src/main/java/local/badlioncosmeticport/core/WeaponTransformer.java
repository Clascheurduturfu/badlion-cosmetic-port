package local.badlioncosmeticport.core;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
/** Adds an early optional replacement; the complete vanilla method remains the fallback. */
public final class WeaponTransformer implements IClassTransformer {
    private static final String HOOK="local/badlioncosmeticport/render/WeaponReplacement";
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null || !"net.minecraft.client.renderer.RenderItem".equals(transformedName)) return bytes;
        ClassNode node=new ClassNode(); new ClassReader(bytes).accept(node,0);
        int hooked=0;
        for(MethodNode method:node.methods) {
            Type[] args=Type.getArgumentTypes(method.desc);
            if(args.length==2 && args[0].getClassName().equals("net.minecraft.item.ItemStack")
                    && args[1].getClassName().endsWith(".IBakedModel") && Type.getReturnType(method.desc).getSort()==Type.VOID) {
                InsnList code=new InsnList(); LabelNode fallback=new LabelNode();
                code.add(new VarInsnNode(Opcodes.ALOAD,1));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"render","(Ljava/lang/Object;)Z",false));
                code.add(new JumpInsnNode(Opcodes.IFEQ,fallback)); code.add(new InsnNode(Opcodes.RETURN)); code.add(fallback); code.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));
                method.instructions.insert(code); hooked++;
            } else {
                int slot=(method.access&Opcodes.ACC_STATIC)==0 ? 1 : 0;
                for(Type arg:args) {
                    if(arg.getClassName().endsWith("ItemCameraTransforms$TransformType")) {
                        InsnList code=new InsnList();code.add(new VarInsnNode(Opcodes.ALOAD,slot));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"mode","(Ljava/lang/Object;)V",false));
                        method.instructions.insert(code); break;
                    }
                    slot+=arg.getSize();
                }
                if(args.length==4 && args[0].getClassName().equals("net.minecraft.item.ItemStack")
                        && args[1].getSort()==Type.INT && args[2].getSort()==Type.INT && args[3].getClassName().endsWith(".IBakedModel")) {
                    InsnList code=new InsnList();code.add(new LdcInsnNode("GUI"));
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"mode","(Ljava/lang/Object;)V",false));method.instructions.insert(code);
                }
            }
        }
        if(hooked!=1) throw new IllegalStateException("Cosmetic weapon RenderItem hook count: "+hooked);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);
        System.out.println("BCP_WEAPON_HOOK RenderItem ready");return writer.toByteArray();
    }
}
