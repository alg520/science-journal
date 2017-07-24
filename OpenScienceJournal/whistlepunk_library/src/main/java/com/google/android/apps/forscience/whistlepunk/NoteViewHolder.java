/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.forscience.whistlepunk.data.GoosciIcon;
import com.google.android.apps.forscience.whistlepunk.data.GoosciSensorAppearance;
import com.google.android.apps.forscience.whistlepunk.filemetadata.Label;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciLabel;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciPictureLabelValue;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciSensorTriggerLabelValue;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciSnapshotValue;
import com.google.android.apps.forscience.whistlepunk.metadata.TriggerHelper;

/**
 * ViewHolder and helper methods for showing notes in a list.
 */
public class NoteViewHolder extends RecyclerView.ViewHolder {
    public TextView durationText;
    public ImageButton menuButton;
    public ImageView captionIcon;
    public View captionView;
    public TextView captionTextView;
    public TextView text;
    public ImageView image;
    public TextView autoText;
    public ViewGroup valuesList;
    public RelativeTimeTextView relativeTimeView;

    public NoteViewHolder(View v) {
        super(v);
        durationText = (TextView) v.findViewById(R.id.time_text);
        menuButton = (ImageButton) v.findViewById(R.id.note_menu_button);
        text = (TextView) v.findViewById(R.id.note_text);
        image = (ImageView) v.findViewById(R.id.note_image);
        autoText = (TextView) v.findViewById(R.id.auto_note_text);
        captionIcon = (ImageView) v.findViewById(R.id.edit_icon);
        captionView = itemView.findViewById(R.id.caption_section);
        captionTextView = (TextView) itemView.findViewById(R.id.caption);
        valuesList = (ViewGroup) itemView.findViewById(R.id.snapshot_values_list);
        relativeTimeView = (RelativeTimeTextView) itemView.findViewById(R.id.relative_time_text);
    }

    public void setNote(Label label, String experimentId) {
        if (label.getType() == GoosciLabel.Label.TEXT) {
            String text = label.getTextLabelValue().text;
            image.setVisibility(View.GONE);
            // No caption, and no caption edit button.
            captionIcon.setVisibility(View.GONE);
            captionView.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(text)) {
                this.text.setText(text);
                this.text.setTextColor(this.text.getResources().getColor(R.color.text_color_black));
            } else {
                this.text.setText(this.text.getResources().getString(
                        R.string.pinned_note_placeholder_text));
                this.text.setTextColor(this.text.getResources().getColor(
                        R.color.text_color_light_grey));
            }
        } else {
            text.setVisibility(View.GONE);
            // Deal with the caption, which is applicable to everything but text labels.
            setupCaption(label.getCaptionText());
        }

        if (label.getType() == GoosciLabel.Label.PICTURE) {
            GoosciPictureLabelValue.PictureLabelValue labelValue = label.getPictureLabelValue();
            image.setVisibility(View.VISIBLE);
            PictureUtils.loadExperimentImage(image.getContext(), image, experimentId,
                    labelValue.filePath);
        } else {
            image.setVisibility(View.GONE);
        }

        if (label.getType() == GoosciLabel.Label.SENSOR_TRIGGER) {
            autoText.setVisibility(View.VISIBLE);
            GoosciSensorTriggerLabelValue.SensorTriggerLabelValue labelValue =
                    label.getSensorTriggerLabelValue();
            // TODO: Load triggers correctly.
            TriggerHelper.populateAutoTextViews(autoText, "TODO", R.drawable.ic_label_black_18dp,
                    autoText.getResources());
        } else {
            autoText.setVisibility(View.GONE);
        }

        if (label.getType() == GoosciLabel.Label.SNAPSHOT) {
            // Add items to valuesList
            loadSnapshotsIntoList(valuesList, label);
        } else {
            valuesList.setVisibility(View.GONE);
        }

        ColorUtils.colorDrawable(captionIcon.getContext(),
                captionIcon.getDrawable(), R.color.text_color_light_grey);
        ColorUtils.colorDrawable(menuButton.getContext(),
                menuButton.getDrawable(), R.color.text_color_light_grey);

    }

    private void setupCaption(String caption) {
        if (!TextUtils.isEmpty(caption)) {
            captionView.setVisibility(View.VISIBLE);
            captionTextView.setText(caption);
            captionIcon.setVisibility(View.GONE);
        } else {
            captionView.setVisibility(View.GONE);
            captionIcon.setVisibility(View.VISIBLE);
        }
    }

    public static void loadSnapshotsIntoList(ViewGroup valuesList, Label label) {
        Context context = valuesList.getContext();
        SensorAppearanceProvider appearanceProvider =
                AppSingleton.getInstance(context).getSensorAppearanceProvider();

        valuesList.setVisibility(View.VISIBLE);
        valuesList.removeAllViews();
        GoosciSnapshotValue.SnapshotLabelValue.SensorSnapshot[] snapshots =
                label.getSnapshotLabelValue().snapshots;
        String valueFormat = context.getResources().getString(
                R.string.data_with_units);
        for (GoosciSnapshotValue.SnapshotLabelValue.SensorSnapshot snapshot : snapshots) {
            ViewGroup snapshotLayout = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.snapshot_value_details, null);
            GoosciSensorAppearance.BasicSensorAppearance appearance =
                    snapshot.sensor.rememberedAppearance;
            ((TextView) snapshotLayout.findViewById(R.id.sensor_name)).setText(appearance.name);
            String value = BuiltInSensorAppearance.formatValue(snapshot.value,
                    appearance.pointsAfterDecimal);
            ((TextView) snapshotLayout.findViewById(R.id.sensor_value)).setText(
                    String.format(valueFormat, value, appearance.units));

            GoosciIcon.IconPath iconPath = appearance.largeIconPath;
            if (iconPath != null) {
                if (iconPath.type == GoosciIcon.IconPath.BUILTIN) {
                    SensorAppearance sa = appearanceProvider.getAppearance(iconPath.pathString);
                    if (sa != null) {
                        SensorAnimationBehavior behavior = sa.getSensorAnimationBehavior();
                        behavior.initializeLargeIcon(
                                ((ImageView) snapshotLayout.findViewById(R.id.large_icon)));
                    }
                }
            }

            valuesList.addView(snapshotLayout);
        }
    }
}