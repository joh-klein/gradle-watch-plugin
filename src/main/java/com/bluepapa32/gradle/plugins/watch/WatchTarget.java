package com.bluepapa32.gradle.plugins.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Named;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.DefaultFileTreeElement;

import static java.util.Collections.addAll;

import static org.gradle.util.CollectionUtils.toStringList;


public class WatchTarget implements Named {

    private String name;
    private List<FileCollection> fileCollections = new ArrayList<>();
    private List<String> tasks = new ArrayList<>();

    public WatchTarget(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public void files(FileCollection files) {
        fileCollections.add(files);
    }

    public void tasks(String... tasks) {
        addAll(this.tasks, tasks);
    }

//  ------------------------------------------------------------- package private

    private long executedAt;

    void setExecutedAt(long executedAt) {
        this.executedAt = executedAt;
    }

    void register(Watcher watcher) throws IOException {
        for (FileCollection files : fileCollections) {
            if (files instanceof DirectoryTree) {
                DirectoryTree dirTree = (DirectoryTree) files;
                watcher.register(dirTree.getDir().toPath());
            } else {
                for (File file : files) {
                    watcher.register(file.toPath());
                }
            }
        }
    }

    boolean isTarget(Path path) {

        File f = path.toFile();

        long lastModified = f.lastModified();
        if (0 < lastModified && lastModified <= executedAt) {
            return false;
        }

        for (FileCollection fileCollection : fileCollections) {

            if (fileCollection instanceof DirectoryTree) {

                DirectoryTree dirTree = (DirectoryTree) fileCollection;

                Path dir = dirTree.getDir().toPath();
                if (!path.startsWith(dir)) {
                    return false;
                }

                String[] segments = toStringList(dir.relativize(path)).toArray(new String[0]);

                return dirTree.getPatterns().getAsSpec().isSatisfiedBy(
                        new DefaultFileTreeElement(f, new RelativePath(true, segments)));
            }

            if (fileCollection instanceof FileTree) {
                return fileCollection.contains(f);
            }

            for (File file : fileCollection) {
                if (path.startsWith(file.toPath())) {
                    return true;
                }
            }
        }

        return false;
    }
}