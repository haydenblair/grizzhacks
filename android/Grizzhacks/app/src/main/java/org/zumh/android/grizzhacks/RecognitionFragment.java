package org.zumh.android.grizzhacks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecognitionFragment extends Fragment {
    private static final String TAG = RecognitionFragment.class.getSimpleName();
    private static final String SAVED_PHOTO_NAME = "grizzhacks_saved_pic.jpg";
    private static final String FILE_TYPE = "image/*";

    private static final int CODE_PICK = 1;
    private static final int CODE_TAKE = 2;

    private final ClarifaiClient mClient = new ClarifaiClient(Credentials.CLIENT_ID, Credentials.CLIENT_SECRET);

    private LinearLayout mLandingLinearLayout;
    private LinearLayout mSelectPictureLinearLayout;
    private LinearLayout mTakePictureLinearLayout;

    private LinearLayout mMainLinearLayout;
    private ImageView mImageView;
    private LinearLayout mChangePhotoLinearLayout;
    private LinearLayout mCopyTagsLinearLayout;
    private GridView mTagsGridView;

    private Uri mPhotoFileUri;
    private List<Tag> mTagsList = new ArrayList<>();
    private List<String> mSelectedTagsList = new ArrayList<>();

    public static RecognitionFragment newInstance() {
        return new RecognitionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recognition, container, false);

        mLandingLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_landing_container);
        mSelectPictureLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_select_pic_ll);
        mSelectPictureLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CODE_PICK);
            }
        });

        mTakePictureLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_take_pic_ll);
        mTakePictureLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File file = new File(Environment.getExternalStorageDirectory(), SAVED_PHOTO_NAME);

                    if (file.exists()) {
                        file.delete();
                    }

                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), R.string.error_creating_file, Toast.LENGTH_SHORT).show();
                    }

                    mPhotoFileUri = Uri.fromFile(file);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoFileUri);
                    startActivityForResult(intent, CODE_TAKE);
                }
            }
        });

        mMainLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_main_container);
        mImageView = (ImageView) view.findViewById(R.id.fragment_recognition_image_view);
        mChangePhotoLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_choose_diff_ll);
        mChangePhotoLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScreenState();
            }
        });

        mCopyTagsLinearLayout = (LinearLayout) view.findViewById(R.id.fragment_recognition_copy_tags_ll);
        mCopyTagsLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder b = new StringBuilder();

                for (String tag : mSelectedTagsList) {
                    b.append(tag + " ");
                }

                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE);

                if (mSelectedTagsList.size() > 0) {
                    b.deleteCharAt(b.length() - 1);
                }

                ClipData clip = ClipData.newPlainText(getString(R.string.app_name), b.toString());
                clipboard.setPrimaryClip(clip);

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType(FILE_TYPE);
                share.setPackage("com.instagram.android");

                share.putExtra(Intent.EXTRA_STREAM, mPhotoFileUri);

                if (share.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(share);
                } else {
                    Toast.makeText(getActivity(), R.string.install_ig, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTagsGridView = (GridView) view.findViewById(R.id.fragment_recognition_tags_gridview);
        mTagsGridView.setAdapter(new TagAdapter(getActivity()));
        mTagsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tag tag = mTagsList.get(position);
                String hashtag = getHashtag(tag.getName());
                LinearLayout ll = (LinearLayout) view.findViewById(R.id.tag_item_container_ll);

                if (mSelectedTagsList.contains(hashtag)) {
                    mSelectedTagsList.remove(hashtag);
                    ll.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.tag));
                } else {
                    mSelectedTagsList.add(hashtag);
                    ll.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.tag_selected));
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == CODE_PICK) {
                toggleScreenState();

                Log.d(TAG, "User picked image: " + data.getData());
                Bitmap bitmap = loadBitmapFromUri(data.getData());
                mPhotoFileUri = data.getData();

                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);

                    new AsyncTask<Bitmap, Void, RecognitionResult>() {
                        @Override
                        protected RecognitionResult doInBackground(Bitmap... params) {
                            return recognizeBitmap(params[0]);
                        }

                        @Override
                        protected void onPostExecute(RecognitionResult result) {
                            updateUIForResult(result);
                        }
                    }.execute(bitmap);
                } else {
                    Toast.makeText(getActivity(), R.string.error_loading_img, Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CODE_TAKE) {
                toggleScreenState();
                Log.d(TAG, "Photo: " + data.getData());

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getApplicationContext().getContentResolver(), mPhotoFileUri);

                    if (bitmap != null) {
                        mImageView.setImageBitmap(bitmap);

                        new AsyncTask<Bitmap, Void, RecognitionResult>() {
                            @Override
                            protected RecognitionResult doInBackground(Bitmap... params) {
                                return recognizeBitmap(params[0]);
                            }

                            @Override
                            protected void onPostExecute(RecognitionResult result) {
                                updateUIForResult(result);
                            }
                        }.execute(bitmap);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Toast.makeText(getActivity(), R.string.error_processing_img, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(uri), null, opts);
            int sampleSize = 1;
            while (opts.outWidth / (2 * sampleSize) >= mImageView.getWidth() &&
                    opts.outHeight / (2 * sampleSize) >= mImageView.getHeight()) {
                sampleSize *= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(uri), null, opts);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + uri, e);
        }

        return null;
    }

    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320, 320 * bitmap.getHeight() / bitmap.getWidth(), true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            return mClient.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }

    private void updateUIForResult(RecognitionResult result) {
        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                mTagsList = cleanTagList(result.getTags());
                ((TagAdapter) mTagsGridView.getAdapter()).notifyDataSetChanged();
                mTagsGridView.invalidateViews();
            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
                Toast.makeText(getActivity(), R.string.error_recognizing_img, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.error_recognizing_img, Toast.LENGTH_SHORT).show();
        }
    }

    private List<Tag> cleanTagList(List<Tag> tags) {
        List<Tag> tagsToReturn = new ArrayList<>();

        for (int i = 0; i < tags.size(); ++i) {
            String name = tags.get(i).getName();
            if (!(name.equals("no person") || name.equals("person"))) {
                tagsToReturn.add(tags.get(i));
            }
        }

        return tagsToReturn;
    }

    private void toggleScreenState() {
        int mainVisibility = mMainLinearLayout.getVisibility();
        int landingVisibility = mLandingLinearLayout.getVisibility();

        mMainLinearLayout.setVisibility(getOppositeVisibility(mainVisibility));
        mLandingLinearLayout.setVisibility(getOppositeVisibility(landingVisibility));

        mTagsList.clear();
        mSelectedTagsList.clear();
    }

    private int getOppositeVisibility(int visibility) {
        return visibility == View.VISIBLE ? View.GONE : View.VISIBLE;
    }

    private String getHashtag(String tag) {
        return "#" + tag.replaceAll(" ", "");
    }

    private class TagAdapter extends BaseAdapter {
        private Context mContext;

        public TagAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mTagsList.size();
        }

        @Override
        public Object getItem(int position) {
            return getHashtag(mTagsList.get(position).getName());
        }

        @Override
        public long getItemId(int position) {
            return mTagsList.get(position).getName().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View gridView = convertView == null ? inflater.inflate(R.layout.tag_item, parent, false) : convertView;

            TextView textView = (TextView) gridView.findViewById(R.id.tag_item_text_view);
            textView.setText(getHashtag(mTagsList.get(position).getName()));

            return gridView;
        }
    }
}
