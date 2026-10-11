package local.badlioncosmeticport;

import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import local.badlioncosmeticport.MeshCosmetics.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Visual Forge browser with unsaved hover previews and persistent selection. */
public final class MeshSelectionScreen extends GuiScreen {
    private final GuiScreen parent;
    private final ThumbnailCache thumbnails=new ThumbnailCache();
    private final List<Pack> matching=new ArrayList<>();
    private GuiTextField search;
    private Category category=Category.CAPE;
    private boolean equippedOnly;
    private int page,columns,rows,gridX,gridY,gridWidth,panelX,panelWidth,cellWidth,cellHeight;
    private float previewYaw=25;
    private Pack previewPack,focused;
    private Loaded preview;
    private String status="";
    public MeshSelectionScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true);buttonList.clear();
        int side=width<600?80:104;panelWidth=width<600?112:170;panelX=width-panelWidth-10;
        gridX=side+16;gridY=63;gridWidth=panelX-gridX-10;
        columns=Math.max(1,gridWidth/82);rows=Math.max(1,(height-38-gridY)/82);
        cellWidth=gridWidth/columns;cellHeight=(height-38-gridY)/rows;
        search=new GuiTextField(0,fontRendererObj,gridX,36,Math.max(40,gridWidth-73),20);
        search.setMaxStringLength(100);
        add(1,10,49,side,"All cosmetics");
        for(Category type:Category.values())add(10+type.ordinal(),10,74+type.ordinal()*Math.min(24,Math.max(18,(height-90)/Category.values().length)),side,label(type));
        add(2,gridX+gridWidth-68,36,68,"All items");
        add(3,gridX,height-29,28,"<");add(4,gridX+gridWidth-28,height-29,28,">");
        add(5,panelX,height-29,panelWidth,"Done");
        add(6,panelX,height-54,panelWidth,"Unequip");
        add(7,panelX,height-78,panelWidth,"Motion: DYNAMIC");
        add(8,panelX,36,25,"<");add(9,panelX+panelWidth-25,36,25,">");
        add(20,panelX,height-102,25,"-");add(21,panelX+panelWidth-25,height-102,25,"+");
        refresh();
    }
    private static String label(Category type) {
        switch(type){case CAPE:return "Cloaks";case HAT:return "Hats";case BANDANA:return "Bandanas";case SHIELD:return "Shields";case PET:return "Pets";case SWORD:return "Swords";case BOW:return "Bows";default:return "Wings";}
    }
    private void add(int id,int x,int y,int w,String text){buttonList.add(new GuiButton(id,x,y,w,20,text));}
    private void refresh() {
        matching.clear();String query=search.getText().toLowerCase(Locale.ROOT);
        for(Category type:Category.values())if(category==null||type==category)
            for(Pack pack:MeshCosmetics.packs(type))if((!equippedOnly||MeshCosmetics.selected(pack))&&(pack.name+" "+pack.id).toLowerCase(Locale.ROOT).contains(query))matching.add(pack);
        page=Math.max(0,Math.min(page,pages()-1));
        for(GuiButton button:buttonList) {
            if(button.id==1)button.enabled=category!=null;
            if(button.id>=10&&button.id<10+Category.values().length)button.enabled=category!=Category.values()[button.id-10];
            if(button.id==2)button.displayString=equippedOnly?"Equipped":"All items";
            if(button.id==3)button.enabled=page>0;
            if(button.id==4)button.enabled=page<pages()-1;
            if(button.id==7){button.visible=category==Category.CAPE||category==Category.PET||category==Category.SHIELD||category==Category.HAT;
                button.enabled=category!=Category.HAT;
                button.displayString=category==Category.CAPE?"Motion: "+MeshCosmetics.capeMotion:category==Category.PET?"Pose: "+MeshCosmetics.petPose:category==Category.SHIELD?"Shield: "+(MeshCosmetics.shieldOnArm?"left arm":"back"):"Height: "+MeshCosmetics.hatHeight+" px";}
            if(button.id==20||button.id==21)button.visible=category==Category.HAT;
        }
    }
    private int pages(){return Math.max(1,(matching.size()+columns*rows-1)/(columns*rows));}
    private Pack card(int mouseX,int mouseY) {
        if(mouseX<gridX||mouseX>=gridX+gridWidth||mouseY<gridY||mouseY>=gridY+rows*cellHeight)return null;
        int col=(mouseX-gridX)/cellWidth,row=(mouseY-gridY)/cellHeight,index=page*columns*rows+row*columns+col;
        if(col>=columns||index>=matching.size())return null;return matching.get(index);
    }
    private void preview(Pack pack) {
        if(pack==previewPack)return;
        if(preview!=null)preview.close();preview=null;previewPack=pack;
        if(pack!=null&&!MeshCosmetics.selected(pack))try{preview=MeshCosmetics.loadPreview(pack);}catch(Exception error){status="Preview unavailable";}
    }
    @Override protected void actionPerformed(GuiButton button) {
        int id=button.id;
        if(id==1||(id>=10&&id<10+Category.values().length)) {
            category=id==1?null:Category.values()[id-10];page=0;focused=null;search.setText("");preview(null);
            previewYaw=category==null||category==Category.CAPE||category==Category.WINGS?25:180;
        }
        if(id==2){equippedOnly=!equippedOnly;page=0;}
        if(id==3)page--;if(id==4)page++;
        if(id==5){mc.displayGuiScreen(parent);return;}
        if(id==6){Category type=focused==null?category:focused.type;if(type!=null)MeshCosmetics.selectId(type,-1,true);focused=null;preview(null);}
        if(id==7) {
            if(category==Category.CAPE)MeshCosmetics.capeMotion=CapeMotion.values()[(MeshCosmetics.capeMotion.ordinal()+1)%3];
            if(category==Category.PET)MeshCosmetics.petPose=MeshCosmetics.petPose.equals("shoulder")?"head":MeshCosmetics.petPose.equals("head")?"air":"shoulder";
            if(category==Category.SHIELD)MeshCosmetics.shieldOnArm=!MeshCosmetics.shieldOnArm;
            MeshCosmetics.save();
        }
        if(id==8)previewYaw-=45;if(id==9)previewYaw+=45;
        if(id==20||id==21){MeshCosmetics.hatHeight=Math.max(-8,Math.min(8,MeshCosmetics.hatHeight+(id==20?-1:1)));MeshCosmetics.save();}
        refresh();
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException {
        search.mouseClicked(x,y,button);Pack pack=card(x,y);
        if(button==0&&pack!=null){preview(null);focused=pack;status=MeshCosmetics.selectId(pack.type,pack.id,true)?"Equipped "+pack.name:"Cannot equip";refresh();}
        super.mouseClicked(x,y,button);
    }
    @Override protected void keyTyped(char typed,int code)throws IOException {
        if(code==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(parent);return;}
        if(search.textboxKeyTyped(typed,code)){page=0;refresh();return;}super.keyTyped(typed,code);
    }
    @Override public void handleMouseInput()throws IOException {
        super.handleMouseInput();int scroll=Mouse.getEventDWheel();if(scroll!=0){page+=scroll>0?-1:1;refresh();}
    }
    @Override public void updateScreen(){search.updateCursorCounter();}
    @Override public boolean doesGuiPauseGame(){return false;}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);preview(null);thumbnails.close();}
    @Override public void drawScreen(int x,int y,float delta) {
        drawDefaultBackground();drawRect(6,7,width-6,30,0xc0142130);
        drawString(fontRendererObj,"Cosmetic Port "+MeshCosmetics.VERSION,13,14,0xffffff);
        drawString(fontRendererObj,matching.size()+" cosmetics",gridX,gridY-11,0xbac9dd);
        search.drawTextBox();if(search.getText().isEmpty()&&!search.isFocused())drawString(fontRendererObj,"Search name or ID",gridX+4,42,0x999999);
        Pack hover=card(x,y);preview(hover!=null?hover:focused);
        int size=columns*rows;
        for(int n=page*size;n<Math.min(matching.size(),(page+1)*size);n++) {
            Pack pack=matching.get(n);int cell=n%size,cx=gridX+cell%columns*cellWidth,cy=gridY+cell/columns*cellHeight;
            boolean equipped=MeshCosmetics.selected(pack);drawRect(cx,cy,cx+cellWidth-5,cy+cellHeight-5,pack==hover?0xe040566d:0xc0202c3c);
            ResourceLocation icon=thumbnails.get(pack);
            if(icon!=null){GlStateManager.color(1,1,1,1);mc.getTextureManager().bindTexture(icon);int edge=Math.min(cellWidth-12,cellHeight-26);
                GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);Gui.drawModalRectWithCustomSizedTexture(cx+(cellWidth-5-edge)/2,cy+3,0,0,edge,edge,edge,edge);}
            String name=fontRendererObj.trimStringToWidth(pack.name,cellWidth-12);
            drawString(fontRendererObj,name,cx+4,cy+cellHeight-20,0xffffff);
            if(equipped)drawString(fontRendererObj,"ON",cx+cellWidth-24,cy+4,0x63e695);
        }
        drawCenteredString(fontRendererObj,(page+1)+" / "+pages(),gridX+gridWidth/2,height-23,0xffffff);
        drawRect(panelX,62,panelX+panelWidth,height-111,0xaa152230);
        if(mc.thePlayer!=null) {
            int factor=new ScaledResolution(mc).getScaleFactor();GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(panelX*factor,111*factor,panelWidth*factor,Math.max(1,height-173)*factor);
            try { MeshCosmetics.withPreview(preview,()->drawPlayer(panelX+panelWidth/2,height-127,Math.min(85,(height-186)/2),previewYaw)); }
            finally {GL11.glDisable(GL11.GL_SCISSOR_TEST);}
        }
        Pack details=previewPack;
        if(details!=null){drawString(fontRendererObj,fontRendererObj.trimStringToWidth(details.name,panelWidth),panelX,height-108,0xffffff);drawString(fontRendererObj,label(details.type)+" #"+details.id,panelX,height-96,0xabc2d9);}
        if(category==Category.SHIELD)drawString(fontRendererObj,"Cosmetic arm attachment",panelX,height-101,0xabc2d9);
        if(!status.isEmpty())drawString(fontRendererObj,fontRendererObj.trimStringToWidth(status,gridWidth),gridX,height-39,0x63e695);
        super.drawScreen(x,y,delta);
    }
    private void drawPlayer(int x,int y,int size,float yaw) {
        if(category==Category.SWORD||category==Category.BOW) {
            Loaded weapon=MeshCosmetics.active(category);
            if(weapon!=null)try {
                mc.getTextureManager().bindTexture(weapon.weaponTexture(category==Category.SWORD?"diamond-gui":"bow-gui"));
                GlStateManager.color(1,1,1,1);GlStateManager.enableBlend();
                int imageSize=Math.min(panelWidth-12,Math.max(40,height-190));
                drawModalRectWithCustomSizedTexture(x-imageSize/2,70,0,0,imageSize,imageSize,imageSize,imageSize);
            }catch(Exception failure){status="Weapon preview unavailable";}
            return;
        }
        EntityPlayerSP player=mc.thePlayer;
        float body=player.renderYawOffset,prevBody=player.prevRenderYawOffset,rotation=player.rotationYaw,pitch=player.rotationPitch,head=player.rotationYawHead,prevHead=player.prevRotationYawHead;
        GlStateManager.pushMatrix();GlStateManager.enableColorMaterial();GlStateManager.translate(x,y,50);GlStateManager.scale(-size,size,size);GlStateManager.rotate(180,0,0,1);
        RenderHelper.enableStandardItemLighting();
        player.renderYawOffset=player.prevRenderYawOffset=yaw;player.rotationYaw=yaw;player.rotationPitch=0;player.rotationYawHead=player.prevRotationYawHead=yaw;
        net.minecraft.client.renderer.entity.RenderManager manager=mc.getRenderManager();manager.setPlayerViewY(180);manager.setRenderShadow(false);
        try {manager.renderEntityWithPosYaw(player,0,0,0,0,1);}
        finally {manager.setRenderShadow(true);player.renderYawOffset=body;player.prevRenderYawOffset=prevBody;player.rotationYaw=rotation;player.rotationPitch=pitch;player.rotationYawHead=head;player.prevRotationYawHead=prevHead;
            GlStateManager.popMatrix();RenderHelper.disableStandardItemLighting();GlStateManager.disableRescaleNormal();GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);GlStateManager.disableTexture2D();GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);}
    }
}
