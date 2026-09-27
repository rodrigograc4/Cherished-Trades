package com.rodrigograc4.cherishedtrades;

public interface CherishedMerchantMenu {

    /** Maps a position in the displayed (favorites-first) list to the server's trade index. */
    int cherishedTrades$getRealIndex(int visualIndex);

    boolean cherishedTrades$isFavorite(int visualIndex);

    /** Bookmarks or un-bookmarks the trade shown at this position and re-sorts the list. */
    void cherishedTrades$toggleFavorite(int visualIndex);
}
