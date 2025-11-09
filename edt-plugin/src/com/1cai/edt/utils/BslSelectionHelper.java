package com.onecai.edt.utils;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Utility for extracting information about the currently selected BSL method.
 *
 * Works both for context menu contributions (where the selection is a
 * {@code com._1c.g5.v8.dt.bsl.model.Method}) and for keyboard shortcuts
 * triggered from the text editor (where the selection is {@link ITextSelection}).
 */
public final class BslSelectionHelper {

    private static final Pattern METHOD_HEADER_PATTERN = Pattern.compile(
        "^(?:&.*\\s+)*\\s*(?i)(процедура|функция|procedure|function)\\s+([\\p{L}\\p{N}_\\.]+)");

    private static final Pattern END_PROCEDURE_PATTERN = Pattern.compile("(?i)^\\s*конецпроцедуры\\b");
    private static final Pattern END_FUNCTION_PATTERN = Pattern.compile("(?i)^\\s*конецфункции\\b");

    private BslSelectionHelper() {}

    public static Optional<BslFunctionInfo> resolve(Object selectionObject, IWorkbenchPart part) {
        ISelection selection = adaptToSelection(selectionObject);

        BslFunctionInfo fromStructured = resolveFromStructuredSelection(selection);
        if (fromStructured != null) {
            return Optional.of(fromStructured);
        }

        if (selection instanceof ITextSelection) {
            BslFunctionInfo fromEditor = resolveFromEditor(
                (ITextSelection) selection,
                part
            );
            if (fromEditor != null) {
                return Optional.of(fromEditor);
            }
        }

        return Optional.empty();
    }

    private static ISelection adaptToSelection(Object selectionObject) {
        if (selectionObject instanceof ISelection) {
            return (ISelection) selectionObject;
        }
        if (selectionObject instanceof IAdaptable) {
            Object adapted = ((IAdaptable) selectionObject).getAdapter(ISelection.class);
            if (adapted instanceof ISelection) {
                return (ISelection) adapted;
            }
        }
        return null;
    }

    private static BslFunctionInfo resolveFromStructuredSelection(ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        Object first = ((IStructuredSelection) selection).getFirstElement();
        if (first == null) {
            return null;
        }

        // Direct support for com._1c.g5.v8.dt.bsl.model.Method via reflection/EMF
        BslFunctionInfo fromMethod = resolveFromMethodObject(first);
        if (fromMethod != null) {
            return fromMethod;
        }

        if (first instanceof ITextSelection) {
            return null;
        }

        // Try to adapt to selection if the first element is ISelection (unlikely)
        if (first instanceof ISelection) {
            return resolveFromStructuredSelection((ISelection) first);
        }

        return null;
    }

    private static BslFunctionInfo resolveFromMethodObject(Object methodObject) {
        String functionName = invokeString(methodObject, "getName", "getMethodName", "getMethod");
        if (functionName == null && methodObject instanceof EObject) {
            functionName = getFeatureString((EObject) methodObject, "name", "methodName");
        }
        if (functionName == null) {
            return null;
        }

        String body = invokeString(methodObject, "getSource", "getSourceCode", "getBody", "getText");
        if (body == null && methodObject instanceof EObject) {
            body = getFeatureString((EObject) methodObject, "source", "text", "body");
        }

        String moduleName = resolveModuleName(methodObject);

        return new BslFunctionInfo(moduleName, functionName, body, inferConfigurationFromModule(moduleName));
    }

    private static String resolveModuleName(Object methodObject) {
        Object module = invokeObject(methodObject, "getModule", "getOwner", "getParent", "getModuleOwner");
        if (module instanceof EObject) {
            String explicit = getFeatureString((EObject) module, "name", "fullName", "moduleName");
            if (explicit != null) {
                return explicit;
            }
        }

        if (module != null) {
            String viaReflection = invokeString(module, "getName", "getFullName");
            if (viaReflection != null) {
                return viaReflection;
            }
        }

        if (methodObject instanceof EObject) {
            EObject container = ((EObject) methodObject).eContainer();
            while (container != null) {
                String value = getFeatureString(container, "name", "fullName", "moduleName");
                if (value != null) {
                    return value;
                }
                container = container.eContainer();
            }
        }

        return null;
    }

    private static BslFunctionInfo resolveFromEditor(ITextSelection selection, IWorkbenchPart part) {
        ITextEditor textEditor = extractTextEditor(part);
        if (textEditor == null) {
            return null;
        }

        IDocument document = textEditor.getDocumentProvider()
            .getDocument(textEditor.getEditorInput());
        if (document == null) {
            return null;
        }

        MethodRegion region = findMethodRegion(document, selection);
        if (region == null) {
            return null;
        }

        String body = region.extractBody(document);
        String moduleName = resolveModuleNameFromEditor(textEditor.getEditorInput());

        return new BslFunctionInfo(
            moduleName,
            region.methodName,
            body,
            inferConfigurationFromModule(moduleName)
        );
    }

    private static ITextEditor extractTextEditor(IWorkbenchPart part) {
        if (part instanceof ITextEditor) {
            return (ITextEditor) part;
        }
        if (part instanceof EditorPart) {
            return ((EditorPart) part).getAdapter(ITextEditor.class);
        }
        if (part instanceof IAdaptable) {
            Object adapted = ((IAdaptable) part).getAdapter(ITextEditor.class);
            if (adapted instanceof ITextEditor) {
                return (ITextEditor) adapted;
            }
        }
        return null;
    }

    private static MethodRegion findMethodRegion(IDocument document, ITextSelection selection) {
        try {
            int startOffset = selection.getOffset();
            if (startOffset < 0) {
                startOffset = 0;
            }
            int startLine = document.getLineOfOffset(startOffset);

            int headerLine = -1;
            String headerText = null;
            for (int line = startLine; line >= 0; line--) {
                String text = document.get(document.getLineOffset(line), document.getLineLength(line));
                String trimmed = text.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("&")) {
                    continue;
                }
                Matcher matcher = METHOD_HEADER_PATTERN.matcher(trimmed);
                if (matcher.find()) {
                    headerLine = line;
                    headerText = trimmed;
                    break;
                }
                if (trimmed.startsWith("Конец")) {
                    // We went past another method end without finding header
                    break;
                }
            }

            if (headerLine < 0 || headerText == null) {
                return null;
            }

            Matcher headerMatcher = METHOD_HEADER_PATTERN.matcher(headerText);
            if (!headerMatcher.find()) {
                return null;
            }

            String keyword = headerMatcher.group(1);
            String methodName = headerMatcher.group(2);

            Pattern endPattern = keyword != null && keyword.toLowerCase().startsWith("ф")
                ? END_FUNCTION_PATTERN
                : END_PROCEDURE_PATTERN;

            int endLine = headerLine;
            for (int line = headerLine + 1; line < document.getNumberOfLines(); line++) {
                String text = document.get(document.getLineOffset(line), document.getLineLength(line));
                if (endPattern.matcher(text.trim()).find()) {
                    endLine = line;
                    break;
                }
            }

            int headerOffset = document.getLineOffset(headerLine);
            int endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);

            return new MethodRegion(methodName, headerOffset, endOffset);
        } catch (BadLocationException e) {
            return null;
        }
    }

    private static String resolveModuleNameFromEditor(IEditorInput editorInput) {
        IFile file = editorInput.getAdapter(IFile.class);
        if (file == null && editorInput instanceof IAdaptable) {
            Object adapted = ((IAdaptable) editorInput).getAdapter(IFile.class);
            if (adapted instanceof IFile) {
                file = (IFile) adapted;
            }
        }
        if (file == null) {
            return null;
        }
        String withoutExt = file.getProjectRelativePath().removeFileExtension().toString();
        return withoutExt.replace('/', '.');
    }

    private static String inferConfigurationFromModule(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return null;
        }
        int idx = moduleName.indexOf('.');
        if (idx > 0) {
            return moduleName.substring(0, idx);
        }
        return moduleName;
    }

    private static Object invokeObject(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.setAccessible(true);
                Object result = method.invoke(target);
                if (result != null) {
                    return result;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static String invokeString(Object target, String... methodNames) {
        Object value = invokeObject(target, methodNames);
        return value != null ? String.valueOf(value) : null;
    }

    private static String getFeatureString(EObject eObject, String... featureNames) {
        EClass eClass = eObject.eClass();
        for (String featureName : featureNames) {
            EStructuralFeature feature = eClass.getEStructuralFeature(featureName);
            if (feature != null) {
                Object value = eObject.eGet(feature);
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }

    private static final class MethodRegion {
        private final String methodName;
        private final int startOffset;
        private final int endOffset;

        private MethodRegion(String methodName, int startOffset, int endOffset) {
            this.methodName = methodName;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        private String extractBody(IDocument document) {
            try {
                return document.get(startOffset, endOffset - startOffset);
            } catch (BadLocationException e) {
                return "";
            }
        }
    }

    /**
     * Immutable DTO with extracted method information.
     */
    public static final class BslFunctionInfo {
        private final String moduleName;
        private final String functionName;
        private final String functionBody;
        private final String configuration;

        public BslFunctionInfo(String moduleName, String functionName, String functionBody, String configuration) {
            this.moduleName = moduleName;
            this.functionName = functionName;
            this.functionBody = functionBody;
            this.configuration = configuration;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public String getFunctionBody() {
            return functionBody;
        }

        public String getConfiguration() {
            return configuration;
        }
    }
}

