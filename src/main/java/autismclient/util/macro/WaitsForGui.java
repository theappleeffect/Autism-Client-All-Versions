package autismclient.util.macro;

public interface WaitsForGui {
    boolean isWaitForGui();
    void setWaitForGui(boolean v);
    String getWaitGuiName();
    void setWaitGuiName(String name);

    default boolean isWaitForGuiChange() { return false; }
}
