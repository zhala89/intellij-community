// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.ui.SdkPathEditor;
import com.intellij.openapi.projectRoots.ui.Util;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.DumbAwareActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author anna
 * @since 26-Dec-2007
 */
public class JavadocOrderRootTypeUIFactory implements OrderRootTypeUIFactory {
  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new JavadocPathsEditor(sdk, FileChooserDescriptorFactory.createMultipleJavaPathDescriptor());
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.JavaDocFolder;
  }

  @Override
  public String getNodeText() {
    return ProjectBundle.message("library.javadocs.node");
  }

  private static class JavadocPathsEditor extends SdkPathEditor {
    private final Sdk mySdk;

    JavadocPathsEditor(Sdk sdk, FileChooserDescriptor descriptor) {
      super(ProjectBundle.message("sdk.configure.javadoc.tab"), JavadocOrderRootType.getInstance(), descriptor);
      mySdk = sdk;
    }

    @Override
    protected void addToolbarButtons(ToolbarDecorator toolbarDecorator) {
      AnActionButton specifyUrlButton = new DumbAwareActionButton(ProjectBundle.message("sdk.paths.specify.url.button"), IconUtil.getAddLinkIcon()) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          onSpecifyUrlButtonClicked();
        }
      };
      specifyUrlButton.setShortcut(CustomShortcutSet.fromString("alt S"));
      specifyUrlButton.addCustomUpdater(e -> myEnabled);
      toolbarDecorator.addExtraAction(specifyUrlButton);
    }

    private void onSpecifyUrlButtonClicked() {
      String defaultDocsUrl = mySdk == null ? "" : StringUtil.notNullize(((SdkType)mySdk.getSdkType()).getDefaultDocumentationUrl(mySdk), "");
      VirtualFile virtualFile = Util.showSpecifyJavadocUrlDialog(myPanel, defaultDocsUrl);
      if (virtualFile != null) {
        addElement(virtualFile);
        setModified(true);
        requestDefaultFocus();
        setSelectedRoots(new Object[]{virtualFile});
      }
    }

    @Override
    protected VirtualFile[] adjustAddedFileSet(Component component, VirtualFile[] files) {
      JavadocQuarantineStatusCleaner.cleanIfNeeded(files);
      List<VirtualFile> docRoots = ContainerUtil.newArrayListWithCapacity(files.length);

      for (VirtualFile file : files) {
        VirtualFile docRoot = null;

        if (file.getName().equalsIgnoreCase("docs")) {
          docRoot = file.findChild("api");
        }
        else if (file.getFileSystem() instanceof ArchiveFileSystem && file.getParent() == null) {
          docRoot = file.findFileByRelativePath("docs/api");
        }

        if (docRoot == null) {
          docRoots.add(file);
        }
        else if (docRoot.findChild("java.base") != null) {
          Stream.of(docRoot.getChildren())
            .filter(f -> f.isDirectory() && f.findChild("module-summary.html") != null)
            .forEach(root -> docRoots.add(root));
        }
        else {
          docRoots.add(docRoot);
        }
      }

      return VfsUtilCore.toVirtualFileArray(docRoots);
    }
  }
}