package comactivesince93.demo.pauseplayaudiorecorder;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MediaRecorderActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnRecord;
    private Button btnPause;
    private Button btnStop;
    private Button btnResume;
    private Button btnPlayAudio;
    private Button btnPauseAudio;
    private SeekBar seekbarAudio;
    private TextView txtAudioCurrentTime;
    private TextView txtAudioDuration;

    private String sampleFileName = "sample";
    private String fileExtension = ".mp3";

    private MediaRecorder recorder;
    private MediaPlayer player;
    private int recordingNum = 0;
    private RecordingState recordingState = RecordingState.IDLE;
    private PlayingState playingState = PlayingState.IDLE;

    private int currentPosition = 0;
    private boolean isMediaPlayerReleased = false;
    private String filePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);

        txtAudioCurrentTime = findViewById(R.id.txtAudioCurrentTime);
        txtAudioDuration = findViewById(R.id.txtAudioDuration);
        seekbarAudio = findViewById(R.id.seekbarAudio);
        btnPlayAudio = findViewById(R.id.playAudioButton);
        btnPauseAudio = findViewById(R.id.pauseAudioButton);
        btnRecord = findViewById(R.id.recordButton);
        btnPause = findViewById(R.id.pauseButton);
        btnStop = findViewById(R.id.stopButton);
        btnResume = findViewById(R.id.resumeButton);

        btnPlayAudio.setOnClickListener(this);
        btnPauseAudio.setOnClickListener(this);
        btnRecord.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnResume.setOnClickListener(this);

        updateUI(recordingState, playingState);
        updateSeekbar();

        filePath = Environment.getExternalStorageDirectory() + "/" + sampleFileName + fileExtension;
        player = new MediaPlayer();
        try {
            player.setDataSource(filePath);
            player.prepare();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    seekbarAudio.setMax(mp.getDuration());
                    txtAudioDuration.setText(getReadableTime(mp.getDuration()));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        seekbarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null && !isMediaPlayerReleased) {
                    player.seekTo(progress);
                    txtAudioCurrentTime.setText(getReadableTime(progress));
                    currentPosition = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void updateSeekbar() {
        final Handler mHandler = new Handler();
        // Make sure you update Seekbar on UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (player != null && !isMediaPlayerReleased) {
                    int mCurrentPosition = player.getCurrentPosition();
                    seekbarAudio.setProgress(mCurrentPosition);
                    txtAudioCurrentTime.setText(getReadableTime(mCurrentPosition));
                }
                mHandler.postDelayed(this, 200);
            }
        });
    }

    private String getReadableTime(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);

        return (new StringBuilder())
                .append(String.format("%02d", minutes))
                .append(":")
                .append(String.format("%02d", seconds))
                .toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pauseButton:
                pauseRecording();

                recordingState = RecordingState.PAUSED;
                updateUI(recordingState, null);
                break;
            case R.id.recordButton:
                resumeRecording();

                recordingState = RecordingState.RECORDING;
                updateUI(recordingState, null);
                break;
            case R.id.resumeButton:
                resumeRecording();

                recordingState = RecordingState.RECORDING;
                updateUI(recordingState, null);
                break;
            case R.id.stopButton:
                stopRecording();

                recordingState = RecordingState.IDLE;
                updateUI(recordingState, null);
                break;
            case R.id.playAudioButton:
                try {
                    File file = new File(filePath);
                    if (!file.exists()) {
                        showToast("File doesn't exists!");
                        return;
                    }

                    playAudio(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                playingState = PlayingState.PLAYING;
                updateUI(null, playingState);
                break;
            case R.id.pauseAudioButton:
                pauseAudio();

                playingState = PlayingState.PAUSED;
                updateUI(null, playingState);
                break;

        }
    }

    private void playAudio(String filePath) throws IOException {
        if (currentPosition != 0) {
            player.seekTo(currentPosition);
        }
        player.start();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isMediaPlayerReleased = true;

                currentPosition = 0;
                mp.release();

                updateUI(null, PlayingState.IDLE);
                seekbarAudio.setProgress(0);
                txtAudioCurrentTime.setText(getReadableTime(0));
            }
        });
    }

    private void showToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private void pauseAudio() {
        player.pause();
        currentPosition = player.getCurrentPosition();
    }

    private void startRecording(String fileName) {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(fileName);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();
    }

    private void stopRecording() {
        pauseRecording();

        String fileName = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/" + sampleFileName + fileExtension;
        File outputFile = new File(fileName);
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 1; i <= recordingNum; i++) {
            String fileName1 = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + sampleFileName + i + fileExtension;
            File file1 = new File(fileName1);
            if (file1.exists()) {
                fileList.add(file1);
            }
        }

        mergeRecordings(outputFile, fileList);
        deleteIndividualRecordings();
    }

    private void resumeRecording() {
        recordingNum += 1;

        String fileName = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/" + sampleFileName + recordingNum + fileExtension;
        File file = new File(fileName);
        if (!file.exists()) {
            startRecording(fileName);
        }
    }

    private void pauseRecording() {
        try {
            if (recordingState != RecordingState.PAUSED) {
                recorder.stop();
                recorder.reset();
                recorder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(RecordingState recordingState, PlayingState playingState) {
        if (recordingState != null)
            switch (recordingState) {
                case IDLE:
                    btnRecord.setVisibility(View.VISIBLE);
                    btnResume.setVisibility(View.GONE);
                    btnPause.setVisibility(View.GONE);
                    btnStop.setVisibility(View.GONE);
                    break;
                case RECORDING:
                    btnRecord.setVisibility(View.GONE);
                    btnResume.setVisibility(View.GONE);
                    btnPause.setVisibility(View.VISIBLE);
                    btnStop.setVisibility(View.VISIBLE);
                    break;
                case PAUSED:
                    btnRecord.setVisibility(View.GONE);
                    btnResume.setVisibility(View.VISIBLE);
                    btnPause.setVisibility(View.GONE);
                    btnStop.setVisibility(View.VISIBLE);
                    break;
            }

        if (playingState != null)
            switch (playingState) {
                case IDLE:
                    btnPlayAudio.setVisibility(View.VISIBLE);
                    btnPauseAudio.setVisibility(View.GONE);
                    break;
                case PAUSED:
                    btnPlayAudio.setVisibility(View.VISIBLE);
                    btnPauseAudio.setVisibility(View.GONE);
                    break;
                case PLAYING:
                    btnPlayAudio.setVisibility(View.GONE);
                    btnPauseAudio.setVisibility(View.VISIBLE);
                    break;
            }
    }

    private void deleteIndividualRecordings() {
        for (int i = 1; i <= recordingNum; i++) {
            String fileName = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + sampleFileName + i + fileExtension;
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void mergeRecordings(File mergedFile, List<File> filesToMerge) {
        try {
            List<Movie> inMovies = new ArrayList<>();
            for (File file : filesToMerge) {
                inMovies.add(MovieCreator.build(file.getPath()));
            }

            List<Track> audioTracks = new LinkedList<>();
            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                }
            }

            Movie result = new Movie();
            if (!audioTracks.isEmpty()) {
                result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            Container out = new DefaultMp4Builder().build(result);
            FileChannel fc = new RandomAccessFile(mergedFile.getPath(), "rw").getChannel();
            out.writeContainer(fc);

            fc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum RecordingState {
        IDLE,
        RECORDING,
        PAUSED
    }

    public enum PlayingState {
        IDLE,
        PLAYING,
        PAUSED
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player != null && !isMediaPlayerReleased) {
            player.release();
            isMediaPlayerReleased = true;
        }
    }
}
