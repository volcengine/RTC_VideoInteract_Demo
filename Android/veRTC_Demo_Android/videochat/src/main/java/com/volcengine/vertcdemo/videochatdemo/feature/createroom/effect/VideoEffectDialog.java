package com.volcengine.vertcdemo.videochatdemo.feature.createroom.effect;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.volcengine.vertcdemo.common.BaseDialog;
import com.volcengine.vertcdemo.videochat.R;
import com.volcengine.vertcdemo.videochatdemo.common.IEffectItemChangedListener;
import com.volcengine.vertcdemo.videochatdemo.common.VideoChatEffectBeautyLayout;
import com.volcengine.vertcdemo.videochatdemo.core.VideoChatRTCManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VideoEffectDialog extends BaseDialog implements IEffectItemChangedListener {

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private SeekBar mSeekbar;
    private VideoChatEffectBeautyLayout mBeautyLayout;
    private EffectFilterLayout mFilterLayout;
    private EffectStickerLayout mStickerLayout;

    private final String mExternalResourcePath;
    public static final String[] TAB_NAMES = {"美颜", "滤镜", "贴纸"};
    private final ArrayList<String> mEffectPathList = new ArrayList<>();

    public VideoEffectDialog(@NonNull Context context) {
        super(context);
        mExternalResourcePath = context.getExternalFilesDir("assets").getAbsolutePath();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.effect_dialog_layout);
        Window window = getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);
        window.setDimAmount(0);
        initUI();
    }

    public void initUI() {
        mViewPager = findViewById(R.id.effect_vp);
        TabViewPageAdapter adapter = new TabViewPageAdapter(Arrays.asList(TAB_NAMES), generateTabViews());
        mViewPager.setAdapter(adapter);

        mTabLayout = findViewById(R.id.effect_tab);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (TextUtils.equals(tab.getText(), "贴纸")) {
                    mSeekbar.setVisibility(View.GONE);
                } else if (TextUtils.equals(tab.getText(), "美颜")) {
                    mSeekbar.setProgress(mBeautyLayout.getEffectProgress(mBeautyLayout.getSelectedId()));
                    mSeekbar.setVisibility(mBeautyLayout.getSelectedId() == R.id.no_select ? View.GONE : View.VISIBLE);
                } else if (TextUtils.equals(tab.getText(), "滤镜")) {
                    mSeekbar.setProgress(mFilterLayout.getEffectProgress(mFilterLayout.getSelectedId()));
                    mSeekbar.setVisibility(mFilterLayout.getSelectedId() == R.id.no_select ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mSeekbar = findViewById(R.id.effect_seekbar);
        mSeekbar.setVisibility(mBeautyLayout.getSelectedId() == R.id.no_select ? View.GONE : View.VISIBLE);
        int currentProgress = mBeautyLayout.getEffectProgress(mBeautyLayout.getSelectedId());
        mSeekbar.setProgress(currentProgress);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int viewId = -1;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                float value = seekBar.getProgress() / 100f;
                View currentView = adapter.getPrimaryItem();
                int tabPos = mTabLayout.getSelectedTabPosition();
                if (tabPos == 0) {
                    VideoChatEffectBeautyLayout effectBeautyLayout = (VideoChatEffectBeautyLayout) currentView;
                    viewId = effectBeautyLayout.getSelectedId();
                    if (viewId == R.id.effect_whiten) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "whiten", value);
                    } else if (viewId == R.id.effect_smooth) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "smooth", value);
                    } else if (viewId == R.id.effect_big_eye) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Eye", value);
                    } else if (viewId == R.id.effect_sharp) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Overall", value);
                    }
                } else if (tabPos == 1) {
                    viewId = ((EffectFilterLayout) currentView).getSelectedId();
                    if (viewId == R.id.effect_landiao) {
                        VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_47_S5");
                    } else if (viewId == R.id.effect_lengyang) {
                        VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_30_Po8");
                    } else if (viewId == R.id.effect_lianai) {
                        VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_24_Po2");
                    } else if (viewId == R.id.effect_yese) {
                        VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_35_L3");
                    }
                    VideoChatRTCManager.ins().updateColorFilterIntensity(viewId == R.id.no_select ? 0 : value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int tabPos = mTabLayout.getSelectedTabPosition();
                if (tabPos == 0) {
                    VideoChatEffectBeautyLayout.sSeekBarProgressMap.put(viewId, seekBar.getProgress());
                } else if (tabPos == 1) {
                    EffectFilterLayout.sSeekBarProgressMap.clear();
                    EffectFilterLayout.sSeekBarProgressMap.put(viewId, seekBar.getProgress());
                }
            }
        });
    }

    public List<View> generateTabViews() {
        List<View> mViews = new ArrayList<>();
        for (String tabName : TAB_NAMES) {
            switch (tabName) {
                case "美颜":
                    mViews.add(mBeautyLayout = new VideoChatEffectBeautyLayout(getContext(), this));
                    break;
                case "滤镜":
                    mViews.add(mFilterLayout = new EffectFilterLayout(getContext(), this));
                    break;
                case "贴纸":
                    mStickerLayout = new EffectStickerLayout(getContext(), this, VideoChatRTCManager.ins().getStickerPath());
                    mViews.add(mStickerLayout);
                    break;
                default:
            }
        }
        return mViews;
    }

    public String getByteStickerPath() {
        File stickerPath = new File(mExternalResourcePath + "/resource/", "cvlab/StickerResource.bundle");
        return stickerPath.getAbsolutePath() + "/";
    }

    public String getByteComposePath() {
        File composerPath = new File(mExternalResourcePath + "/resource/", "cvlab/ComposeMakeup.bundle");
        return composerPath.getAbsolutePath() + "/ComposeMakeup/beauty_Android_live";
    }

    public String getByteShapePath() {
        File composerPath = new File(mExternalResourcePath + "/resource/", "cvlab/ComposeMakeup.bundle");
        return composerPath.getAbsolutePath() + "/ComposeMakeup/reshape_live";
    }

    public String getByteColorFilterPath() {
        File filterPath = new File(mExternalResourcePath + "/resource/", "cvlab/FilterResource.bundle");
        return filterPath.getAbsolutePath() + "/Filter/";
    }


    @Override
    public void onChanged(View newItem, View lastItem) {
        if (newItem.getId() == R.id.no_select) {
            mSeekbar.setVisibility(View.GONE);
        } else if (mTabLayout.getSelectedTabPosition() != 2) {
            mSeekbar.setVisibility(View.VISIBLE);
        }

        if (mTabLayout.getSelectedTabPosition() == 0) {
            int currentProgress = mBeautyLayout.getEffectProgress(newItem.getId());
            mSeekbar.setProgress(currentProgress);

            VideoChatEffectBeautyLayout.sSeekBarProgressMap.put(newItem.getId(), currentProgress);
            mBeautyLayout.updateStatusByValue();
            if (newItem.getId() == R.id.no_select) {
                VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "whiten", 0);
                VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "smooth", 0);
                VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Eye", 0);
                VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Overall", 0);
            } else {
                for (Map.Entry<Integer, Integer> entry : VideoChatEffectBeautyLayout.sSeekBarProgressMap.entrySet()) {

                    float value = entry.getValue() == null ? 0 : entry.getValue();
                    int id = newItem.getId();
                    if (id == R.id.effect_whiten) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "whiten", value / 100);
                    } else if (id == R.id.effect_smooth) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteComposePath(), "smooth", value / 100);
                    } else if (id == R.id.effect_big_eye) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Eye", value / 100);
                    } else if (id == R.id.effect_sharp) {
                        VideoChatRTCManager.ins().updateVideoEffectNode(getByteShapePath(), "Internal_Deform_Overall", value / 100);
                    }
                }
            }
        } else if (mTabLayout.getSelectedTabPosition() == 1) {
            int currentProgress = mFilterLayout.getEffectProgress(newItem.getId());
            mSeekbar.setProgress(currentProgress);
            for (Map.Entry<Integer, Integer> entry : EffectFilterLayout.sSeekBarProgressMap.entrySet()) {
                if (entry.getKey() != newItem.getId()) {
                    entry.setValue(0);
                }
            }

            if (newItem.getId() == R.id.effect_landiao) {
                VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_47_S5");
            } else if (newItem.getId() == R.id.effect_lengyang) {
                VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_30_Po8");
            } else if (newItem.getId() == R.id.effect_lianai) {
                VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_24_Po2");
            } else if (newItem.getId() == R.id.effect_yese) {
                VideoChatRTCManager.ins().setVideoEffectColorFilter(getByteColorFilterPath() + "Filter_35_L3");
            }
            VideoChatRTCManager.ins().updateColorFilterIntensity((float) currentProgress / 100);
        } else if (mTabLayout.getSelectedTabPosition() == 2) {
            int id = newItem.getId();
            if (id == R.id.effect_manhuanansheng) {
                VideoChatRTCManager.ins().setStickerNodes(EffectStickerLayout.KEY_STICKER_NAME_CARTOON_BOY);
            } else if (id == R.id.effect_shaonvmanhua) {
                VideoChatRTCManager.ins().setStickerNodes(EffectStickerLayout.KEY_STICKER_NAME_CARTOON_GIRL);
            } else if (id == R.id.effect_suixingshan) {
                VideoChatRTCManager.ins().setStickerNodes(EffectStickerLayout.KEY_STICKER_NAME_STAR_BLING);
            } else if (id == R.id.effect_fuguyanjing) {
                VideoChatRTCManager.ins().setStickerNodes(EffectStickerLayout.KEY_STICKER_NAME_RETRO_GLASSES);
            } else if (id == R.id.no_select) {
                VideoChatRTCManager.ins().setStickerNodes("");
            }
        }
    }
}
