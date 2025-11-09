package com.onecai.edt.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.google.gson.JsonObject;
import com.onecai.edt.services.BackendConnector;
import com.onecai.edt.utils.BslSelectionHelper;
import com.onecai.edt.utils.BslSelectionHelper.BslFunctionInfo;

/**
 * Context menu action: Analyze Function with AI
 */
public class AnalyzeFunctionAction implements IObjectActionDelegate {

    private IWorkbenchPart targetPart;
    private ISelection currentSelection;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

    @Override
    public void run(IAction action) {
        if (currentSelection == null) {
            MessageDialog.openWarning(
                targetPart.getSite().getShell(),
                "Analyze Function",
                "No function selected"
            );
            return;
        }

        BslFunctionInfo info = BslSelectionHelper.resolve(currentSelection, targetPart).orElse(null);
        if (info == null || info.getFunctionName() == null || info.getModuleName() == null) {
            MessageDialog.openWarning(
                targetPart.getSite().getShell(),
                "Analyze Function",
                "Could not determine function info"
            );
            return;
        }

        // Call backend
        analyzeFunction(info.getModuleName(), info.getFunctionName());
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        currentSelection = selection;
    }

    private void analyzeFunction(String moduleName, String functionName) {
        BackendConnector backend = new BackendConnector();

        // Show progress dialog
        MessageDialog.openInformation(
            targetPart.getSite().getShell(),
            "Analyze Function",
            "Analyzing function: " + functionName + "\n" +
            "Module: " + moduleName + "\n\n" +
            "This will query Neo4j for dependencies..."
        );

        // TODO: Real implementation
        new Thread(() -> {
            try {
                JsonObject result = backend.analyzeDependencies(moduleName, functionName);

                Display.getDefault().asyncExec(() -> {
                    if (result != null) {
                        showAnalysisResult(functionName, result);
                    } else {
                        MessageDialog.openError(
                            targetPart.getSite().getShell(),
                            "Error",
                            "Failed to analyze function. Check backend connection."
                        );
                    }
                });

            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(
                        targetPart.getSite().getShell(),
                        "Error",
                        "Analysis error: " + e.getMessage()
                    );
                });
            }
        }).start();
    }

    private void showAnalysisResult(String functionName, JsonObject result) {
        // Format result
        StringBuilder message = new StringBuilder();
        message.append("Анализ функции: ").append(functionName).append("\n\n");
        
        if (result.has("result")) {
            JsonObject resultData = result.getAsJsonObject("result");
            message.append("Результат:\n");
            message.append(resultData.toString());
        } else {
            message.append("Нет результатов");
        }

        MessageDialog.openInformation(
            targetPart.getSite().getShell(),
            "Analysis Result",
            message.toString()
        );
    }
}







