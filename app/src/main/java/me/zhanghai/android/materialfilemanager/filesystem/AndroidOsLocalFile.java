/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialfilemanager.filesystem;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.WorkerThread;

import org.threeten.bp.Instant;

import java.util.List;
import java.util.Objects;

import me.zhanghai.android.materialfilemanager.functional.Functional;
import me.zhanghai.android.materialfilemanager.functional.FunctionalException;
import me.zhanghai.android.materialfilemanager.functional.throwing.ThrowingFunction;

public class AndroidOsLocalFile extends LocalFile {

    private AndroidOs.Information mInformation;

    private JavaFileObserver mObserver;

    public AndroidOsLocalFile(Uri path) {
        super(path);
    }

    private AndroidOsLocalFile(Uri path, AndroidOs.Information information) {
        super(path);

        mInformation = information;
    }

    @WorkerThread
    public void loadInformation() throws FileSystemException {
        mInformation = AndroidOs.loadInformation(mPath.getPath());
    }

    @Override
    public long getSize() {
        return mInformation.size;
    }

    @Override
    public Instant getLastModified() {
        return mInformation.lastModificationTime;
    }

    @Override
    public boolean isDirectory() {
        // TODO: Symbolic link?
        return mInformation.type == PosixFileType.DIRECTORY;
    }

    @Override
    @WorkerThread
    public List<File> getFileList() throws FileSystemException {
        List<java.io.File> javaFiles = JavaFile.listFiles(makeJavaFile());
        List<AndroidOs.Information> informations;
        try {
            informations = Functional.map(javaFiles, (ThrowingFunction<java.io.File,
                    AndroidOs.Information>) javaFile -> AndroidOs.loadInformation(javaFile.getPath()));
        } catch (FunctionalException e) {
            throw e.getCauseAs(FileSystemException.class);
        }
        return Functional.map(javaFiles, (JavaOs, index) -> new AndroidOsLocalFile(
                Uri.fromFile(JavaOs), informations.get(index)));
    }

    @Override
    public void startObserving(Runnable observer) {
        if (mObserver != null) {
            throw new IllegalStateException("Already observing");
        }
        mObserver = new JavaFileObserver(mPath.getPath(), observer);
        mObserver.startWatching();
    }

    @Override
    public boolean isObserving() {
        return mObserver != null;
    }

    @Override
    public void stopObserving() {
        if (mObserver != null) {
            mObserver.stopWatching();
            mObserver = null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AndroidOsLocalFile that = (AndroidOsLocalFile) object;
        return Objects.equals(mPath, that.mPath)
                && Objects.equals(mInformation, that.mInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPath, mInformation);
    }


    public static final Creator<AndroidOsLocalFile> CREATOR = new Creator<AndroidOsLocalFile>() {
        @Override
        public AndroidOsLocalFile createFromParcel(Parcel source) {
            return new AndroidOsLocalFile(source);
        }
        @Override
        public AndroidOsLocalFile[] newArray(int size) {
            return new AndroidOsLocalFile[size];
        }
    };

    protected AndroidOsLocalFile(Parcel in) {
        super(in);

        mInformation = in.readParcelable(AndroidOs.Information.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeParcelable(mInformation, flags);
    }
}
