package autismclient.util;

public interface PackUtilSpecialGuiActions {
    void packutil$closeWithPacket();

    void packutil$closeWithoutPacket();

    void packutil$desync();

    default void packutil$closeWithPacket(boolean notify) {
        packutil$closeWithPacket();
    }

    default void packutil$closeWithoutPacket(boolean notify) {
        packutil$closeWithoutPacket();
    }

    default void packutil$desync(boolean notify) {
        packutil$desync();
    }
}
