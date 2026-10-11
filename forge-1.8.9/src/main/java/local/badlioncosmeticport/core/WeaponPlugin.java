package local.badlioncosmeticport.core;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.Name("CosmeticPortWeapons")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"local.badlioncosmeticport.core."})
public final class WeaponPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[]{"local.badlioncosmeticport.core.WeaponTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public String getAccessTransformerClass() { return null; }
    public void injectData(Map<String,Object> data) {}
}
