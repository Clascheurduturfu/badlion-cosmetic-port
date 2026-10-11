import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class GsonSchemaCheck {
    public static final class Cosmetic {
        boolean active;
        long cosmeticId;
        String cosmeticType;
        JsonObject cosmeticData;
        JsonObject sourceData;
        JsonObject globalData;
    }
    public static final class Response {
        List<Cosmetic> cosmetics;
    }
    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        try {
            gson.fromJson("{\"cosmetics\":[{\"active\":0}]}", Response.class);
            throw new AssertionError("Numeric active must fail in the client's Gson");
        } catch (JsonSyntaxException expected) {
            System.out.println("PASS reproduced original numeric/boolean failure");
        }
        String payload = new String(Files.readAllBytes(Paths.get(args[0])), "UTF-8");
        Response response = gson.fromJson(payload, Response.class);
        if (response.cosmetics.size() != 3397) throw new AssertionError("Wrong record count");
        for (Cosmetic cosmetic : response.cosmetics) {
            if (cosmetic.active || cosmetic.cosmeticType == null || cosmetic.cosmeticId < 0)
                throw new AssertionError("Invalid record");
            if (cosmetic.cosmeticData == null || cosmetic.sourceData == null || cosmetic.globalData == null)
                throw new AssertionError("Missing per-cosmetic JSON objects");
        }
        System.out.println("PASS client's Gson parses all 3397 records from native-hook output");
    }
}
