/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.button;

import com.google.android.material.R;

import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ToggleButton;
import androidx.annotation.BoolRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import com.google.android.material.button.MaterialButton.OnPressedChangeListener;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.shape.AbsoluteCornerSize;
import com.google.android.material.shape.CornerSize;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.StateListCornerSize;
import com.google.android.material.shape.StateListShapeAppearanceModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A common container for a set of related, toggleable {@link MaterialButton}s. The {@link
 * MaterialButton}s in this group will be shown on a single line.
 *
 * <p>This layout currently only supports child views of type {@link MaterialButton}. Buttons can be
 * added to this view group via XML, as follows:
 *
 * <pre>
 * &lt;com.google.android.material.button.MaterialButtonToggleGroup
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:id="@+id/toggle_button_group"
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"&gt;
 *
 *     &lt;com.google.android.material.button.MaterialButton
 *         style="?attr/materialButtonOutlinedStyle"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="@string/button_label_private"/&gt;
 *     &lt;com.google.android.material.button.MaterialButton
 *         style="?attr/materialButtonOutlinedStyle"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="@string/button_label_team"/&gt;
 *     &lt;com.google.android.material.button.MaterialButton
 *         style="?attr/materialButtonOutlinedStyle"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="@string/button_label_everyone"/&gt;
 *     &lt;com.google.android.material.button.MaterialButton
 *         style="?attr/materialButtonOutlinedStyle"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="@string/button_label_custom"/&gt;
 *
 * &lt;/com.google.android.material.button.MaterialButtonToggleGroup&gt;
 * </pre>
 *
 * <p>Buttons can also be added to this view group programmatically via the {@link #addView(View)}
 * methods.
 *
 * <p>Note: Styling must applied to each child button individually. It is recommended to use the
 * {@code materialButtonOutlinedStyle} attribute for all child buttons. {@code
 * materialButtonOutlinedStyle} will most closely match the Material Design guidelines for this
 * component, and supports the checked state for child buttons.
 *
 * <p>Any {@link MaterialButton}s added to this view group are automatically marked as {@code
 * checkable}, and by default multiple buttons within the same group can be checked. To enforce that
 * only one button can be checked at a time, set the {@code app:singleSelection} attribute to {@code
 * true} on the MaterialButtonToggleGroup or call {@link #setSingleSelection(boolean)
 * setSingleSelection(true)}.
 *
 * <p>MaterialButtonToggleGroup is a {@link LinearLayout}. Using {@code
 * android:layout_width="MATCH_PARENT"} and removing {@code android:insetBottom} {@code
 * android:insetTop} on the children is recommended if using {@code VERTICAL}.
 *
 * <p>In order to cohesively group multiple buttons together, MaterialButtonToggleGroup overrides
 * the start and end margins of any children added to this layout such that child buttons are placed
 * directly adjacent to one another.
 *
 * <p>MaterialButtonToggleGroup also overrides any {@code shapeAppearance}, {@code
 * shapeAppearanceOverlay}, or {@code cornerRadius} attribute set on MaterialButton children such
 * that only the left-most corners of the first child and the right-most corners of the last child
 * retain their shape appearance or corner size.
 *
 * <p>For more information, see the <a
 * href="https://github.com/material-components/material-components-android/blob/master/docs/components/Button.md">component
 * developer guidance</a> and <a href="https://material.io/components/buttons/overview">design
 * guidelines</a>.
 */
public class MaterialButtonToggleGroup extends LinearLayout {

  /**
   * Interface definition for a callback to be invoked when a {@link MaterialButton} is checked or
   * unchecked in this group.
   */
  public interface OnButtonCheckedListener {
    /**
     * Called when a {@link MaterialButton} in this group is checked or unchecked.
     *
     * @param group The group in which the MaterialButton's checked state was changed
     * @param checkedId The unique identifier of the MaterialButton whose check state changed
     * @param isChecked Whether the MaterialButton is currently checked
     */
    void onButtonChecked(MaterialButtonToggleGroup group, @IdRes int checkedId, boolean isChecked);
  }

  private static final String LOG_TAG = "MButtonToggleGroup";
  private static final int DEF_STYLE_RES =
      R.style.Widget_MaterialComponents_MaterialButtonToggleGroup;

  private final List<ShapeAppearanceModel> originalChildShapeAppearanceModels = new ArrayList<>();
  private final List<StateListShapeAppearanceModel> originalChildStateListShapeAppearanceModels =
      new ArrayList<>();

  private final PressedStateTracker pressedStateTracker = new PressedStateTracker();
  private final LinkedHashSet<OnButtonCheckedListener> onButtonCheckedListeners =
      new LinkedHashSet<>();
  private final Comparator<MaterialButton> childOrderComparator =
      (v1, v2) -> {
        int checked = Boolean.valueOf(v1.isChecked()).compareTo(v2.isChecked());
        if (checked != 0) {
          return checked;
        }

        int stateful = Boolean.valueOf(v1.isPressed()).compareTo(v2.isPressed());
        if (stateful != 0) {
          return stateful;
        }

        // don't return 0s
        return Integer.compare(indexOfChild(v1), indexOfChild(v2));
      };

  private Integer[] childOrder;
  private boolean skipCheckedStateTracker = false;
  private boolean singleSelection;
  private boolean selectionRequired;

  @NonNull private StateListCornerSize innerCornerSize;
  @Nullable private StateListShapeAppearanceModel groupStateListShapeAppearance;
  @Px private int spacing;

  @IdRes private final int defaultCheckId;
  private Set<Integer> checkedIds = new HashSet<>();

  public MaterialButtonToggleGroup(@NonNull Context context) {
    this(context, null);
  }

  public MaterialButtonToggleGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.materialButtonToggleGroupStyle);
  }

  public MaterialButtonToggleGroup(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(wrap(context, attrs, defStyleAttr, DEF_STYLE_RES), attrs, defStyleAttr);
    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();
    TypedArray attributes =
        ThemeEnforcement.obtainStyledAttributes(
            context, attrs, R.styleable.MaterialButtonToggleGroup, defStyleAttr, DEF_STYLE_RES);

    setSingleSelection(
        attributes.getBoolean(R.styleable.MaterialButtonToggleGroup_singleSelection, false));
    defaultCheckId =
        attributes.getResourceId(R.styleable.MaterialButtonToggleGroup_checkedButton, View.NO_ID);
    selectionRequired =
        attributes.getBoolean(R.styleable.MaterialButtonToggleGroup_selectionRequired, false);

    if (attributes.hasValue(R.styleable.MaterialButtonToggleGroup_shapeAppearance)) {
      groupStateListShapeAppearance =
          StateListShapeAppearanceModel.create(
              context, attributes, R.styleable.MaterialButtonToggleGroup_shapeAppearance);
      if (groupStateListShapeAppearance == null) {
        groupStateListShapeAppearance =
            new StateListShapeAppearanceModel.Builder(
                    ShapeAppearanceModel.builder(
                            context,
                            attributes.getResourceId(
                                R.styleable.MaterialButtonToggleGroup_shapeAppearance, 0),
                            attributes.getResourceId(
                                R.styleable.MaterialButtonToggleGroup_shapeAppearanceOverlay, 0))
                        .build())
                .build();
      }
    }
    innerCornerSize =
        StateListCornerSize.create(
            context,
            attributes,
            R.styleable.MaterialButtonToggleGroup_innerCornerSize,
            new AbsoluteCornerSize(0));

    spacing =
        attributes.getDimensionPixelSize(R.styleable.MaterialButtonToggleGroup_android_spacing, 0);

    setChildrenDrawingOrderEnabled(true);
    setEnabled(attributes.getBoolean(R.styleable.MaterialButtonToggleGroup_android_enabled, true));
    attributes.recycle();

    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    if (defaultCheckId != View.NO_ID) {
      updateCheckedIds(Collections.singleton(defaultCheckId));
    }
  }

  @Override
  protected void dispatchDraw(@NonNull Canvas canvas) {
    updateChildOrder();
    super.dispatchDraw(canvas);
  }

  /**
   * This override prohibits Views other than {@link MaterialButton} to be added. It also makes
   * updates to the add button shape and margins.
   */
  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    if (!(child instanceof MaterialButton)) {
      Log.e(LOG_TAG, "Child views must be of type MaterialButton.");
      return;
    }

    super.addView(child, index, params);
    MaterialButton buttonChild = (MaterialButton) child;
    setGeneratedIdIfNeeded(buttonChild);
    // Sets sensible default values and an internal checked change listener for this child
    setupButtonChild(buttonChild);

    // Update button group's checked states
    checkInternal(buttonChild.getId(), buttonChild.isChecked());

    // Saves original child shape appearance.
    originalChildShapeAppearanceModels.add(buttonChild.getShapeAppearanceModel());
    originalChildStateListShapeAppearanceModels.add(buttonChild.getStateListShapeAppearanceModel());

    // Enable children based on the MaterialButtonToggleGroup own isEnabled
    buttonChild.setEnabled(isEnabled());

    ViewCompat.setAccessibilityDelegate(
        buttonChild,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setCollectionItemInfo(
                CollectionItemInfoCompat.obtain(
                    /* rowIndex= */ 0,
                    /* rowSpan= */ 1,
                    /* columnIndex= */ getIndexWithinVisibleButtons(host),
                    /* columnSpan= */ 1,
                    /* heading= */ false,
                    /* selected= */ ((MaterialButton) host).isChecked()));
          }
        });
  }

  @Override
  public void onViewRemoved(View child) {
    super.onViewRemoved(child);

    if (child instanceof MaterialButton) {
      ((MaterialButton) child).setOnPressedChangeListenerInternal(null);
    }

    int indexOfChild = indexOfChild(child);
    if (indexOfChild >= 0) {
      originalChildShapeAppearanceModels.remove(indexOfChild);
      originalChildStateListShapeAppearanceModels.remove(indexOfChild);
    }

    updateChildShapes();
    adjustChildMarginsAndUpdateLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    updateChildShapes();
    adjustChildMarginsAndUpdateLayout();
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);
    AccessibilityNodeInfoCompat infoCompat = AccessibilityNodeInfoCompat.wrap(info);
    infoCompat.setCollectionInfo(
        CollectionInfoCompat.obtain(
            /* rowCount= */ 1,
            /* columnCount= */ getVisibleButtonCount(),
            /* hierarchical= */ false,
            /* selectionMode= */ isSingleSelection()
                ? CollectionInfoCompat.SELECTION_MODE_SINGLE
                : CollectionInfoCompat.SELECTION_MODE_MULTIPLE));
  }

  /**
   * Sets the {@link MaterialButton} whose id is passed in to the checked state. If this
   * MaterialButtonToggleGroup is in {@link #isSingleSelection() single selection mode}, then all
   * other MaterialButtons in this group will be unchecked. Otherwise, other MaterialButtons will
   * retain their checked state.
   *
   * @param id View ID of {@link MaterialButton} to set checked
   * @see #uncheck(int)
   * @see #clearChecked()
   * @see #getCheckedButtonIds()
   * @see #getCheckedButtonId()
   */
  public void check(@IdRes int id) {
    checkInternal(id, true);
  }

  /**
   * Sets the {@link MaterialButton} whose id is passed in to the unchecked state.
   *
   * @param id View ID of {@link MaterialButton} to set unchecked
   * @see #check(int)
   * @see #clearChecked()
   * @see #getCheckedButtonIds()
   * @see #getCheckedButtonId()
   */
  public void uncheck(@IdRes int id) {
    checkInternal(id, false);
  }

  /**
   * Clears the selections. When the selections are cleared, no {@link MaterialButton} in this group
   * is checked and {@link #getCheckedButtonIds()} returns an empty list.
   *
   * @see #check(int)
   * @see #uncheck(int)
   * @see #getCheckedButtonIds()
   * @see #getCheckedButtonId()
   */
  public void clearChecked() {
    updateCheckedIds(new HashSet<Integer>());
  }

  /**
   * When in {@link #isSingleSelection() single selection mode}, returns the identifier of the
   * selected button in this group. Upon empty selection, the returned value is {@link View#NO_ID}.
   * If not in single selection mode, the return value is {@link View#NO_ID}.
   *
   * @return The unique id of the selected {@link MaterialButton} in this group in {@link
   *     #isSingleSelection() single selection mode}. When not in {@link #isSingleSelection() single
   *     selection mode}, returns {@link View#NO_ID}.
   * @see #check(int)
   * @see #uncheck(int)
   * @see #clearChecked()
   * @see #getCheckedButtonIds()
   * @attr ref R.styleable#MaterialButtonToggleGroup_checkedButton
   */
  @IdRes
  public int getCheckedButtonId() {
    return singleSelection && !checkedIds.isEmpty() ? checkedIds.iterator().next() : View.NO_ID;
  }

  /**
   * Returns the identifiers of the selected {@link MaterialButton}s in this group. Upon empty
   * selection, the returned value is an empty list.
   *
   * @return The unique IDs of the selected {@link MaterialButton}s in this group. When in {@link
   *     #isSingleSelection() single selection mode}, returns a list with a single ID. When no
   *     {@link MaterialButton}s are selected, returns an empty list.
   * @see #check(int)
   * @see #uncheck(int)
   * @see #clearChecked()
   * @see #getCheckedButtonId()
   */
  @NonNull
  public List<Integer> getCheckedButtonIds() {
    List<Integer> orderedCheckedIds = new ArrayList<>();
    for (int i = 0; i < getChildCount(); i++) {
      int childId = getChildButton(i).getId();
      if (checkedIds.contains(childId)) {
        orderedCheckedIds.add(childId);
      }
    }

    return orderedCheckedIds;
  }

  /**
   * Add a listener that will be invoked when the check state of a {@link MaterialButton} in this
   * group changes. See {@link OnButtonCheckedListener}.
   *
   * <p>Components that add a listener should take care to remove it when finished via {@link
   * #removeOnButtonCheckedListener(OnButtonCheckedListener)}.
   *
   * @param listener listener to add
   */
  public void addOnButtonCheckedListener(@NonNull OnButtonCheckedListener listener) {
    onButtonCheckedListeners.add(listener);
  }

  /**
   * Remove a listener that was previously added via {@link
   * #addOnButtonCheckedListener(OnButtonCheckedListener)}.
   *
   * @param listener listener to remove
   */
  public void removeOnButtonCheckedListener(@NonNull OnButtonCheckedListener listener) {
    onButtonCheckedListeners.remove(listener);
  }

  /** Remove all previously added {@link OnButtonCheckedListener}s. */
  public void clearOnButtonCheckedListeners() {
    onButtonCheckedListeners.clear();
  }

  /**
   * Returns whether this group only allows a single button to be checked.
   *
   * @return whether this group only allows a single button to be checked
   * @attr ref R.styleable#MaterialButtonToggleGroup_singleSelection
   */
  public boolean isSingleSelection() {
    return singleSelection;
  }

  /**
   * Sets whether this group only allows a single button to be checked.
   *
   * <p>Calling this method results in all the buttons in this group to become unchecked.
   *
   * @param singleSelection whether this group only allows a single button to be checked
   * @attr ref R.styleable#MaterialButtonToggleGroup_singleSelection
   */
  public void setSingleSelection(boolean singleSelection) {
    if (this.singleSelection != singleSelection) {
      this.singleSelection = singleSelection;
      clearChecked();
    }
    updateChildrenA11yClassName();
  }

  private void updateChildrenA11yClassName() {
    for (int i = 0; i < getChildCount(); i++) {
      String className =
          singleSelection ? RadioButton.class.getName() : ToggleButton.class.getName();
      getChildButton(i).setA11yClassName(className);
    }
  }

  /**
   * Sets whether we prevent all child buttons from being deselected.
   *
   * @attr ref R.styleable#MaterialButtonToggleGroup_selectionRequired
   */
  public void setSelectionRequired(boolean selectionRequired) {
    this.selectionRequired = selectionRequired;
  }

  /**
   * Returns whether we prevent all child buttons from being deselected.
   *
   * @attr ref R.styleable#MaterialButtonToggleGroup_selectionRequired
   */
  public boolean isSelectionRequired() {
    return selectionRequired;
  }

  /**
   * Sets whether this group only allows a single button to be checked.
   *
   * <p>Calling this method results in all the buttons in this group to become unchecked.
   *
   * @param id boolean resource ID of whether this group only allows a single button to be checked
   * @attr ref R.styleable#MaterialButtonToggleGroup_singleSelection
   */
  public void setSingleSelection(@BoolRes int id) {
    setSingleSelection(getResources().getBoolean(id));
  }

  /** Returns the spacing (in pixels) between each button in the group. */
  @Px
  public int getSpacing() {
    return spacing;
  }

  /**
   * Sets the spacing between each button in the group.
   *
   * @param spacing the spacing (in pixels) between each button in the group
   */
  public void setSpacing(@Px int spacing) {
    this.spacing = spacing;
    invalidate();
    requestLayout();
  }

  /** Returns the inner corner size of the group. */
  @NonNull
  public CornerSize getInnerCornerSize() {
    return innerCornerSize.getDefaultCornerSize();
  }

  /**
   * Sets the inner corner size of the group.
   *
   * <p>Can set as an {@link AbsoluteCornerSize} or {@link RelativeCornerSize}. Don't set relative
   * corner size larger than 50% or absolute corner size larger than half height to avoid corner
   * overlapping.
   *
   * @param cornerSize the inner corner size of the group
   */
  public void setInnerCornerSize(@NonNull CornerSize cornerSize) {
    innerCornerSize = StateListCornerSize.create(cornerSize);
    updateChildShapes();
    invalidate();
  }

  /**
   * Returns the inner corner size state list of the group.
   *
   * @hide
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public StateListCornerSize getInnerCornerSizeStateList() {
    return innerCornerSize;
  }

  /**
   * Sets the inner corner size state list of the group.
   *
   * <p>Can set as an {@link StateListCornerSize}. Don't set relative corner size larger than 50% or
   * absolute corner size larger than half height to the corner size in any state to avoid corner
   * overlapping.
   *
   * @param cornerSizeStateList the inner corner size state list of the group
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public void setInnerCornerSizeStateList(@NonNull StateListCornerSize cornerSizeStateList) {
    innerCornerSize = cornerSizeStateList;
    updateChildShapes();
    invalidate();
  }

  /** Returns the {@link ShapeAppearanceModel} of the group. */
  @Nullable
  public ShapeAppearanceModel getShapeAppearance() {
    return groupStateListShapeAppearance == null
        ? null
        : groupStateListShapeAppearance.getDefaultShape(/* withCornerSizeOverrides= */ true);
  }

  /**
   * Sets the {@link ShapeAppearanceModel} of the group.
   *
   * @param shapeAppearance The new {@link ShapeAppearanceModel} of the group.
   */
  public void setShapeAppearance(@Nullable ShapeAppearanceModel shapeAppearance) {
    groupStateListShapeAppearance =
        new StateListShapeAppearanceModel.Builder(shapeAppearance).build();
    updateChildShapes();
    invalidate();
  }

  /**
   * Returns the {@link StateListShapeAppearanceModel} of the group.
   *
   * @hide
   */
  @Nullable
  @RestrictTo(Scope.LIBRARY_GROUP)
  public StateListShapeAppearanceModel getStateListShapeAppearance() {
    return groupStateListShapeAppearance;
  }

  /**
   * Sets the {@link StateListShapeAppearanceModel} of the group.
   *
   * @param stateListShapeAppearance The new {@link StateListShapeAppearanceModel} of the group.
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public void setStateListShapeAppearance(
      @Nullable StateListShapeAppearanceModel stateListShapeAppearance) {
    groupStateListShapeAppearance = stateListShapeAppearance;
    updateChildShapes();
    invalidate();
  }

  private void setCheckedStateForView(@IdRes int viewId, boolean checked) {
    View checkedView = findViewById(viewId);
    if (checkedView instanceof MaterialButton) {
      skipCheckedStateTracker = true;
      ((MaterialButton) checkedView).setChecked(checked);
      skipCheckedStateTracker = false;
    }
  }

  /**
   * Sets a negative marginStart on all but the first child, if two adjacent children both have a
   * stroke width greater than 0. This prevents a double-width stroke from being drawn for two
   * adjacent stroked children, and instead draws the adjacent strokes directly on top of each
   * other.
   *
   * <p>The negative margin adjustment amount will be equal to the smaller of the two adjacent
   * stroke widths.
   *
   * <p>Also rearranges children such that they are shown in the correct visual order.
   */
  private void adjustChildMarginsAndUpdateLayout() {
    int firstVisibleChildIndex = getFirstVisibleChildIndex();
    if (firstVisibleChildIndex == -1) {
      return;
    }

    for (int i = firstVisibleChildIndex + 1; i < getChildCount(); i++) {
      // Only adjusts margins if both adjacent children are MaterialButtons
      MaterialButton currentButton = getChildButton(i);
      MaterialButton previousButton = getChildButton(i - 1);

      // Calculates the margin adjustment to be the smaller of the two adjacent stroke widths
      int smallestStrokeWidth = 0;
      if (spacing <= 0) {
        smallestStrokeWidth = min(currentButton.getStrokeWidth(), previousButton.getStrokeWidth());
      }

      LayoutParams params = buildLayoutParams(currentButton);
      if (getOrientation() == HORIZONTAL) {
        params.setMarginEnd(0);
        params.setMarginStart(spacing - smallestStrokeWidth);
        params.topMargin = 0;
      } else {
        params.bottomMargin = 0;
        params.topMargin = spacing - smallestStrokeWidth;
        params.setMarginStart(0);
      }

      currentButton.setLayoutParams(params);
    }

    resetChildMargins(firstVisibleChildIndex);
  }

  @NonNull
  private MaterialButton getChildButton(int index) {
    return (MaterialButton) getChildAt(index);
  }

  private void resetChildMargins(int childIndex) {
    if (getChildCount() == 0 || childIndex == -1) {
      return;
    }

    MaterialButton currentButton = getChildButton(childIndex);
    LayoutParams params = (LayoutParams) currentButton.getLayoutParams();
    if (getOrientation() == VERTICAL) {
      params.topMargin = 0;
      params.bottomMargin = 0;
      return;
    }

    params.setMarginEnd(0);
    params.setMarginStart(0);
    params.leftMargin = 0;
    params.rightMargin = 0;
  }

  /**
   * Sets all corner radii override to inner corner size except for leftmost and rightmost corners.
   */
  @VisibleForTesting
  void updateChildShapes() {
    int childCount = getChildCount();
    int firstVisibleChildIndex = getFirstVisibleChildIndex();
    int lastVisibleChildIndex = getLastVisibleChildIndex();
    for (int i = 0; i < childCount; i++) {
      MaterialButton button = getChildButton(i);
      if (button.getVisibility() == GONE) {
        continue;
      }
      boolean isFirstVisible = i == firstVisibleChildIndex;
      boolean isLastVisible = i == lastVisibleChildIndex;

      StateListShapeAppearanceModel.Builder originalStateListShapeBuilder =
          getOriginalStateListShapeBuilder(isFirstVisible, isLastVisible, i);
      // Determines which corners of the original shape should be kept.
      boolean isHorizontal = getOrientation() == HORIZONTAL;
      boolean isRtl = ViewUtils.isLayoutRtl(this);
      int cornerPositionBitsToKeep = 0;
      if (isHorizontal) {
        // When horizontal (ltr), keeps the left two original corners for the first button.
        if (isFirstVisible) {
          cornerPositionBitsToKeep |=
              StateListShapeAppearanceModel.CORNER_TOP_LEFT
                  | StateListShapeAppearanceModel.CORNER_BOTTOM_LEFT;
        }
        // When horizontal (ltr), keeps the right two original corners for the last button.
        if (isLastVisible) {
          cornerPositionBitsToKeep |=
              StateListShapeAppearanceModel.CORNER_TOP_RIGHT
                  | StateListShapeAppearanceModel.CORNER_BOTTOM_RIGHT;
        }
        // If rtl, swap the position bits of left corners and right corners.
        if (isRtl) {
          cornerPositionBitsToKeep =
              StateListShapeAppearanceModel.swapCornerPositionRtl(cornerPositionBitsToKeep);
        }
      } else {
        // When vertical, keeps the top two original corners for the first button.
        if (isFirstVisible) {
          cornerPositionBitsToKeep |=
              StateListShapeAppearanceModel.CORNER_TOP_LEFT
                  | StateListShapeAppearanceModel.CORNER_TOP_RIGHT;
        }
        // When vertical, keeps the bottom two original corners for the last button.
        if (isLastVisible) {
          cornerPositionBitsToKeep |=
              StateListShapeAppearanceModel.CORNER_BOTTOM_LEFT
                  | StateListShapeAppearanceModel.CORNER_BOTTOM_RIGHT;
        }
      }
      // Overrides the corners that don't need to keep with unary operator.
      int cornerPositionBitsToOverride = ~cornerPositionBitsToKeep;
      StateListShapeAppearanceModel newStateListShape =
          originalStateListShapeBuilder
              .setCornerSizeOverride(innerCornerSize, cornerPositionBitsToOverride)
              .build();
      if (newStateListShape.isStateful()) {
        button.setStateListShapeAppearanceModel(newStateListShape);
      } else {
        button.setShapeAppearanceModel(
            newStateListShape.getDefaultShape(/* withCornerSizeOverrides= */ true));
      }
    }
  }

  /**
   * Returns a {@link StateListShapeAppearanceModel.Builder} as the original shape of a child
   * button.
   *
   * <p>It takes the group shape, if specified, as the original state list shape for the first and
   * last buttons. Otherwise, it takes the state list shape (or build one from the shape appearance
   * model, if state list shape is not specified) in the child button.
   *
   * @param isFirstVisible Whether this is the first visible child button regardless its index.
   * @param isLastVisible Whether this is the last visible child button regardless its index.
   * @param index The index of the child button.
   */
  @NonNull
  private StateListShapeAppearanceModel.Builder getOriginalStateListShapeBuilder(
      boolean isFirstVisible, boolean isLastVisible, int index) {
    StateListShapeAppearanceModel originalStateList =
        groupStateListShapeAppearance != null && (isFirstVisible || isLastVisible)
            ? groupStateListShapeAppearance
            : originalChildStateListShapeAppearanceModels.get(index);
    // If the state list shape is not specified, creates one from the shape appearance model.
    return originalStateList == null
        ? new StateListShapeAppearanceModel.Builder(originalChildShapeAppearanceModels.get(index))
        : originalStateList.toBuilder();
  }

  private int getFirstVisibleChildIndex() {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      if (isChildVisible(i)) {
        return i;
      }
    }

    return -1;
  }

  private int getLastVisibleChildIndex() {
    int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
      if (isChildVisible(i)) {
        return i;
      }
    }

    return -1;
  }

  private boolean isChildVisible(int i) {
    View child = getChildAt(i);
    return child.getVisibility() != View.GONE;
  }

  private int getVisibleButtonCount() {
    int count = 0;
    for (int i = 0; i < getChildCount(); i++) {
      if (this.getChildAt(i) instanceof MaterialButton && isChildVisible(i)) {
        count++;
      }
    }
    return count;
  }

  private int getIndexWithinVisibleButtons(@Nullable View child) {
    if (!(child instanceof MaterialButton)) {
      return -1;
    }
    int index = 0;
    for (int i = 0; i < getChildCount(); i++) {
      if (this.getChildAt(i) == child) {
        return index;
      }
      if (this.getChildAt(i) instanceof MaterialButton && isChildVisible(i)) {
        index++;
      }
    }
    return -1;
  }

  private void checkInternal(@IdRes int buttonId, boolean checked) {
    if (buttonId == View.NO_ID) {
      Log.e(LOG_TAG, "Button ID is not valid: " + buttonId);
      return;
    }
    Set<Integer> checkedIds = new HashSet<>(this.checkedIds);
    if (checked && !checkedIds.contains(buttonId)) {
      if (singleSelection && !checkedIds.isEmpty()) {
        checkedIds.clear();
      }
      checkedIds.add(buttonId);
    } else if (!checked && checkedIds.contains(buttonId)) {
      // Do not uncheck button if no selection remains when selection is required.
      if (!selectionRequired || checkedIds.size() > 1) {
        checkedIds.remove(buttonId);
      }
    } else {
      // No state change, do nothing
      return;
    }
    updateCheckedIds(checkedIds);
  }

  private void updateCheckedIds(Set<Integer> checkedIds) {
    Set<Integer> previousCheckedIds = this.checkedIds;
    this.checkedIds = new HashSet<>(checkedIds);
    for (int i = 0; i < getChildCount(); i++) {
      int buttonId = getChildButton(i).getId();
      setCheckedStateForView(buttonId, checkedIds.contains(buttonId));
      if (previousCheckedIds.contains(buttonId) != checkedIds.contains(buttonId)) {
        dispatchOnButtonChecked(buttonId, checkedIds.contains(buttonId));
      }
    }
    invalidate();
  }

  private void dispatchOnButtonChecked(@IdRes int buttonId, boolean checked) {
    for (OnButtonCheckedListener listener : onButtonCheckedListeners) {
      listener.onButtonChecked(this, buttonId, checked);
    }
  }

  private void setGeneratedIdIfNeeded(@NonNull MaterialButton materialButton) {
    // Generates an ID if none is set, for relative positioning purposes
    if (materialButton.getId() == View.NO_ID) {
      materialButton.setId(View.generateViewId());
    }
  }

  /**
   * Sets sensible default values for {@link MaterialButton} child of this group, set child to
   * {@code checkable}, and set internal checked change listener for this child.
   *
   * @param buttonChild {@link MaterialButton} child to set up to be added to this {@link
   *     MaterialButtonToggleGroup}
   */
  private void setupButtonChild(@NonNull MaterialButton buttonChild) {
    buttonChild.setMaxLines(1);
    buttonChild.setEllipsize(TruncateAt.END);
    buttonChild.setCheckable(true);

    buttonChild.setOnPressedChangeListenerInternal(pressedStateTracker);

    // Enables surface layer drawing for semi-opaque strokes
    buttonChild.setShouldDrawSurfaceColorStroke(true);
  }

  @NonNull
  private LinearLayout.LayoutParams buildLayoutParams(@NonNull View child) {
    ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
    if (layoutParams instanceof LinearLayout.LayoutParams) {
      return (LayoutParams) layoutParams;
    }

    return new LayoutParams(layoutParams.width, layoutParams.height);
  }

  /**
   * We keep track of which views are pressed and checked to draw them last. This prevents visual
   * issues with overlapping strokes.
   */
  @Override
  protected int getChildDrawingOrder(int childCount, int i) {
    if (childOrder == null || i >= childOrder.length) {
      Log.w(LOG_TAG, "Child order wasn't updated");
      return i;
    }

    return childOrder[i];
  }

  private void updateChildOrder() {
    final SortedMap<MaterialButton, Integer> viewToIndexMap = new TreeMap<>(childOrderComparator);
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      viewToIndexMap.put(getChildButton(i), i);
    }

    childOrder = viewToIndexMap.values().toArray(new Integer[0]);
  }

  void onButtonCheckedStateChanged(@NonNull MaterialButton button, boolean isChecked) {
    // Checked state change is triggered by the button group, do not update checked ids again.
    if (skipCheckedStateTracker) {
      return;
    }
    checkInternal(button.getId(), isChecked);
  }

  /**
   * Enables this {@link MaterialButtonToggleGroup} and all its {@link MaterialButton} children
   *
   * @param enabled boolean to setEnable {@link MaterialButtonToggleGroup}
   */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    // Enable or disable child buttons
    for (int i = 0; i < getChildCount(); i++) {
      MaterialButton childButton = getChildButton(i);
      childButton.setEnabled(enabled);
    }
  }

  private class PressedStateTracker implements OnPressedChangeListener {

    @Override
    public void onPressedChanged(@NonNull MaterialButton button, boolean isPressed) {
      invalidate();
    }
  }
}
