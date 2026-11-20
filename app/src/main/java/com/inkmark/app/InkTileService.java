package com.inkmark.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * 控制中心磁贴：一次性动作按钮。
 *
 * <p>它不是一个开关，因此永远保持 STATE_INACTIVE —— 点一下就是"开一次标注"，
 * 不会像开关那样点亮成"已开启"，也不会因为标注层还在前台就显示为激活态，
 * 避免用户误以为磁贴是个可以开关的模式。</p>
 */
public class InkTileService extends TileService {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        Intent intent = new Intent(this, AnnotateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending);
            } else {
                startActivityAndCollapse(intent);
            }
        } catch (Throwable t) {
            try {
                startActivity(intent);
            } catch (Throwable ignored) {
            }
        }

        // 动作型磁贴：点击后立刻回到未激活外观。部分 ROM 会先闪一下按下态，
        // 这里延迟一帧再拉回，避免残留"已开启"的观感。
        updateTile();
        main.postDelayed(this::updateTile, 240);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_label));
        try {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile));
        } catch (Throwable ignored) {
        }
        tile.updateTile();
    }
}
