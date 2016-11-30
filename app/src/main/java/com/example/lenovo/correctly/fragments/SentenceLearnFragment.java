package com.example.lenovo.correctly.fragments;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.correctly.MainActivity;
import com.example.lenovo.correctly.R;
import com.example.lenovo.correctly.clients.StreamingRecognizeClient;

import java.io.InputStream;
import java.util.Locale;

import io.grpc.ManagedChannel;


public class SentenceLearnFragment extends Fragment {
    public int MainIterator=0;
    public String sentence = "this is [part 1] and [here another] and [another one]";
    private final int SPEECH_RECOGNITION_CODE = 1;
    public TextView textView;
    public TextView EnglishText;
    public TextView FrenchText;
    public TextView mAction;
    // public EditText editText;
    public RatingBar ratingBar;
    String text = "";
    TextToSpeech t1;
    private SpannableStringBuilder sb;
    private TextView txtOutput;
    private ImageButton btnMicrophone;
    private ImageButton btnPlay;
    private View myFragmentView;
    public int i=0;
    public String wordToCheck="";
    public String[] list_of_wordsFrench = new String[]{"Bonjour voici \n" +
            "le magasin"};

    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    public boolean flag_play=false;
    public boolean flag_record=false;

    private AudioRecord mAudioRecord = null;
    private Thread mRecordingThread = null;
    private boolean mIsRecording = false;
    private ImageButton mRecordingBt;
    private TextView mConsoleMsg;
    private StreamingRecognizeClient mStreamingClient;
    private int mBufferSize;
    public String confidence="";
    public String transcript="";
    public String[] listOfWords;
    public String[] list_of_wordsEnglish=new String[]{"Good morning, here is the store"};

    public SentenceLearnFragment() {
        // Required empty public constructor
    }
    public int wTC_one=0;
    public int wTC_two=0;

    private void startRecording() {
        mAudioRecord.startRecording();

        mRecordingThread = new Thread(new Runnable() {
            public void run() {
                readData();
            }
        }, "AudioRecorder Thread");
        mRecordingThread.start();
    }

    private SpannableStringBuilder addClickablePart(String str) {

        listOfWords=str.split(" ");

        sb = new SpannableStringBuilder(str);
        int idx1 =0;
        int idx2 = 0;
        for(int i=0;i<listOfWords.length; i++)
        {

            idx1=str.indexOf(listOfWords[i]);
            idx2 = str.lastIndexOf(listOfWords[i], idx1) +listOfWords[i].length() ;

            final String clickString = str.substring(idx1, idx2);
            sb.setSpan(new ClickableSpan() {

                @Override
                public void onClick(View widget) {
                    if(!mIsRecording)
                        t1.speak(clickString, TextToSpeech.QUEUE_FLUSH, null);
                }
            }, idx1, idx2, 0);

        }

        return sb;
    }



    public void changeColorOfWord()
    {

        try {
            String str = FrenchText.getText().toString();
            int idx1 = wTC_one;
            int idx2 = wTC_two;

            //SpannableStringBuilder spannable = new SpannableStringBuilder(str);
            str = str.replace(" ", "");
            transcript = transcript.replace("\"", "");
            transcript = transcript.replace(" ", "");
            float confidenceF = Float.parseFloat(confidence);
            Toast.makeText(getContext(), "confidence= " + confidenceF + "\ntranscript= " + transcript, Toast.LENGTH_SHORT).show();
            if (Float.parseFloat(confidence) >= 0.7&&(transcript
                    .compareToIgnoreCase(wordToCheck)==0))
                sb.setSpan(new ForegroundColorSpan(Color.parseColor("#008744")), wTC_one, wTC_two, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                sb.setSpan(new ForegroundColorSpan(Color.parseColor("#d62d20")), wTC_one, wTC_two, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            FrenchText.setText(sb);
            MainIterator++;
            after_micClick();
        }
        catch (Exception e)
        {
            Toast.makeText(getContext(), "wTC_one= " + wTC_one + "\nwTC_two= " + wTC_two, Toast.LENGTH_SHORT).show();
        }

    }



    private void readData() {
        byte sData[] = new byte[mBufferSize];
        while (mIsRecording) {
            int bytesRead = mAudioRecord.read(sData, 0, mBufferSize);
            if (bytesRead > 0) {
                try {
                    mStreamingClient.recognizeBytes(sData, bytesRead);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(getClass().getSimpleName(), "Error while reading bytes: " + bytesRead);
            }
        }
    }


    private void initialize() {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg != null) {

                    String[] help=msg.obj.toString().split("\n");
                    for(int i=0;i<help.length;i++)
                    {
                        if(help[i].contains("transcript:")) {
                            transcript = help[i].replace("transcript:", "");
                            mConsoleMsg.setText("\ntranscript: " + transcript
                                    + mConsoleMsg.getText());
                        }

                        if(help[i].contains("confidence:"))
                        {
                            confidence=help[i].replace("confidence:","");
                            transcript=help[i-1].replace("transcript:","");
                            mConsoleMsg.setText("\ntranscript: "+transcript +
                                    "\nconfidence:"+confidence +
                                    mConsoleMsg.getText());
                            mIsRecording = false;
                            mAudioRecord.stop();
                            mStreamingClient.finish();
                            Toast.makeText(getContext(),"confidence= "+confidence+"\ntranscript= " +transcript,Toast.LENGTH_SHORT).show();
                            changeColorOfWord();

                        }
                    }
                }
                super.handleMessage(msg);
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {

                // Required to support Android 4.x.x (patches for OpenSSL from Google-Play-Services)
                try {
                    ProviderInstaller.installIfNeeded(getContext());
                } catch (GooglePlayServicesRepairableException e) {

                    // Indicates that Google Play services is out of date, disabled, etc.
                    e.printStackTrace();
                    // Prompt the user to install/update/enable Google Play services.
                    GooglePlayServicesUtil.showErrorNotification(
                            e.getConnectionStatusCode(), getContext());
                    return;

                } catch (GooglePlayServicesNotAvailableException e) {
                    // Indicates a non-recoverable error; the ProviderInstaller is not able
                    // to install an up-to-date Provider.
                    e.printStackTrace();
                    return;
                }

                try {
                    InputStream credentials = getActivity().getAssets().open
                            ("credentials.json");
                    ManagedChannel channel = StreamingRecognizeClient.createChannel(
                            HOSTNAME, PORT, credentials);
                    mStreamingClient = new StreamingRecognizeClient(channel,
                            RECORDER_SAMPLERATE, handler);
                } catch (Exception e) {
                    Log.e(MainActivity.class.getSimpleName(), "Error", e);
                }
            }
        }).start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStreamingClient != null) {
            try {
                mStreamingClient.shutdown();
            } catch (InterruptedException e) {
                Log.e(MainActivity.class.getSimpleName(), "Error", e);
            }
        }
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        myFragmentView = inflater.inflate(R.layout.fragment_sentence_learn, container, false);
        EnglishText = (TextView) myFragmentView.findViewById(R.id.TranslationText);
        FrenchText = (TextView) myFragmentView.findViewById(R.id.ChallengeText);
        mAction = (TextView) myFragmentView.findViewById(R.id.mAction);

        FrenchText.setMovementMethod(LinkMovementMethod.getInstance());
        FrenchText.setText(addClickablePart(sentence), TextView.BufferType.SPANNABLE);
// Span to set text color to some RGB value
        final ForegroundColorSpan fcs = new ForegroundColorSpan(Color.rgb(158, 158, 158));

        EnglishText.setText(list_of_wordsEnglish[0]);
        FrenchText.setText(list_of_wordsFrench[0]);
        FrenchText.setText(addClickablePart(list_of_wordsFrench[0]));
        // editText.setText(sb);




        t1 = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    //
                    t1.setLanguage(Locale.FRENCH);

                }
            }
        });
        btnPlay = (ImageButton) myFragmentView.findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               /* i++;
                MainIterator=0;

                if(i<list_of_wordsFrench.length) {
                    FrenchText.setText(list_of_wordsFrench[i]);
                    EnglishText.setText(list_of_wordsEnglish[i]);
                }
                else {
                    i = 0;
                    FrenchText.setText(list_of_wordsFrench[i]);
                    EnglishText.setText(list_of_wordsEnglish[i]);
                }*/

                String toSpeak = FrenchText.getText().toString();
                if(!mIsRecording)
                    t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
                SpannableStringBuilder ssb=addClickablePart(FrenchText.getText().toString());
                FrenchText.setText(ssb);

            }
        });

        mBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat
                .CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                mBufferSize);

        initialize();

        mRecordingBt = (ImageButton) myFragmentView.findViewById(R.id.btn_mic);
        mConsoleMsg = (TextView) myFragmentView.findViewById(R.id.mConsoleMsg);
        mRecordingBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!t1.isSpeaking()) {

                    if (mIsRecording) {
                        mIsRecording = false;
                        mAudioRecord.stop();
                        mStreamingClient.finish();

                    } else {
                        MainIterator = 0;
                        after_micClick();
                    }
                }
            }
        });


        // Inflate the layout for this fragment
        return myFragmentView;
    }


    public void after_micClick() {
        mIsRecording = true;
        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            // mRecordingBt.setText(R.string.stop_recording);



            if (MainIterator < listOfWords.length) {

                String str = FrenchText.getText().toString();
                int idx1 = 0;
                int idx2 = 0;

                wTC_one = str.indexOf(listOfWords[MainIterator]);
                wTC_two = str.lastIndexOf(listOfWords[MainIterator], wTC_one) + listOfWords[MainIterator].length();
                wordToCheck = listOfWords[MainIterator];
                wordToCheck=wordToCheck.replace(" ","");
                wordToCheck=wordToCheck.replace(" ","");
                sb.setSpan(new StyleSpan(Typeface.BOLD),wTC_one, wTC_two, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                FrenchText.setText(sb);
                startRecording();
            }
            else
                mIsRecording=false;
        } else {
            Log.i(this.getClass().getSimpleName(), "Not Initialized yet.");
        }
    }




}