package de.markusfisch.android.shadereditor.fragment;

import de.markusfisch.android.shadereditor.activity.AddUniformActivity;
import de.markusfisch.android.shadereditor.opengl.ShaderRenderer;
import de.markusfisch.android.shadereditor.view.SoftKeyboard;
import de.markusfisch.android.shadereditor.widget.TextureParametersView;
import de.markusfisch.android.shadereditor.R;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Pattern;

public abstract class SamplerPropertiesFragment extends Fragment {
	public static final String TEXTURE_NAME_PATTERN = "[a-zA-Z0-9_]+";
	public static final String SAMPLER_2D = "sampler2D";
	public static final String SAMPLER_CUBE = "samplerCube";

	private static final Pattern NAME_PATTERN = Pattern.compile(
			"^" + TEXTURE_NAME_PATTERN + "$");

	private static boolean inProgress = false;

	private TextView sizeCaption;
	private SeekBar sizeBarView;
	private TextView sizeView;
	private EditText nameView;
	private CheckBox addUniformView;
	private TextureParametersView textureParameterView;
	private View progressView;
	private String samplerType = SAMPLER_2D;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_sampler_properties, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save:
				saveSamplerAsync();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void setSizeCaption(String caption) {
		sizeCaption.setText(caption);
	}

	protected void setMaxValue(int max) {
		sizeBarView.setMax(max);
	}

	protected void setSamplerType(String name) {
		samplerType = name;
	}

	protected abstract int saveSampler(
			Context context,
			String name,
			int size);

	protected View initView(
			Activity activity,
			LayoutInflater inflater,
			ViewGroup container) {
		View view = inflater.inflate(
				R.layout.fragment_sampler_properties,
				container,
				false);

		sizeCaption = view.findViewById(R.id.size_caption);
		sizeBarView = view.findViewById(R.id.size_bar);
		sizeView = view.findViewById(R.id.size);
		nameView = view.findViewById(R.id.name);
		addUniformView = view.findViewById(
				R.id.should_add_uniform);
		textureParameterView = view.findViewById(
				R.id.texture_parameters);
		progressView = view.findViewById(R.id.progress_view);

		if (activity.getCallingActivity() == null) {
			addUniformView.setVisibility(View.GONE);
			addUniformView.setChecked(false);
			textureParameterView.setVisibility(View.GONE);
		}

		initSizeView();
		initNameView();

		return view;
	}

	private void initSizeView() {
		setSizeView(sizeBarView.getProgress());
		sizeBarView.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(
					SeekBar seekBar,
					int progressValue,
					boolean fromUser) {
				setSizeView(progressValue);
			}

			@Override
			public void onStartTrackingTouch(
					SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(
					SeekBar seekBar) {
			}
		});
	}

	private void setSizeView(int power) {
		int size = getPower(power);
		sizeView.setText(String.format(
				Locale.US,
				"%d x %d",
				size,
				size));
	}

	private void initNameView() {
		nameView.setFilters(new InputFilter[]{
				new InputFilter() {
					@Override
					public CharSequence filter(
							CharSequence source,
							int start,
							int end,
							Spanned dest,
							int dstart,
							int dend) {
						return NAME_PATTERN
								.matcher(source)
								.find() ? null : "";
					}
				}});
	}

	private void saveSamplerAsync() {
		final Context context = getActivity();

		if (context == null || inProgress) {
			return;
		}

		final String name = nameView.getText().toString();
		final String params = textureParameterView.getTextureParams();

		if (name.trim().length() < 1) {
			Toast.makeText(
					context,
					R.string.missing_name,
					Toast.LENGTH_SHORT).show();

			return;
		} else if (!name.matches(TEXTURE_NAME_PATTERN) ||
				name.equals(ShaderRenderer.UNIFORM_BACKBUFFER)) {
			Toast.makeText(
					context,
					R.string.invalid_texture_name,
					Toast.LENGTH_SHORT).show();

			return;
		}

		SoftKeyboard.hide(context, nameView);

		final int size = getPower(sizeBarView.getProgress());

		inProgress = true;
		progressView.setVisibility(View.VISIBLE);

		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... nothings) {
				return saveSampler(context, name, size);
			}

			@Override
			protected void onPostExecute(Integer messageId) {
				inProgress = false;
				progressView.setVisibility(View.GONE);

				Activity activity = getActivity();
				if (activity == null) {
					return;
				}

				if (messageId > 0) {
					Toast.makeText(
							activity,
							messageId,
							Toast.LENGTH_SHORT).show();

					return;
				}

				if (addUniformView.isChecked()) {
					AddUniformActivity.setAddUniformResult(
							activity,
							"uniform " + samplerType + " " + name + ";" +
									params);
				}

				activity.finish();
			}
		}.execute();
	}

	private static int getPower(int power) {
		return 1 << (power + 1);
	}
}
