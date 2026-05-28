package org.openbeans.claude.netbeans;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;

import javax.swing.text.StyledDocument;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression coverage for the caret-listener NPE that froze NetBeans when the
 * active editor showed a non-local FileObject (e.g. a class opened from a JAR).
 * {@link org.openide.filesystems.FileUtil#toFile} returns null for those
 * FileObjects; the payload builder must short-circuit instead of dereferencing.
 */
public class SelectionChangedPayloadTest {

    @Test
    public void returnsNullForNonLocalFileObject() {
        // given: a handler whose toLocalFile() simulates a non-local FileObject
        NetBeansMCPHandler handler = new NetBeansMCPHandler() {
            @Override
            File toLocalFile(FileObject fileObject) {
                return null;
            }
        };

        // when
        ObjectNode params = handler.buildSelectionChangedParams(
                null, (StyledDocument) null, "anything", 0, 0);

        // then: no NPE, and caller is told to skip the notification
        assertNull(params);
    }
}
