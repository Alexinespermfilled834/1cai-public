package com.onecai.edt.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onecai.edt.services.BackendConnector;
import com.onecai.edt.utils.BslSelectionHelper;
import com.onecai.edt.utils.BslSelectionHelper.BslFunctionInfo;
import com.onecai.edt.views.MetadataGraphView;

/**
 * Context menu action: Show Call Graph
 */
public class ShowCallGraphAction implements IObjectActionDelegate {

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
                "Show Call Graph",
                "No function selected"
            );
            return;
        }

        BslFunctionInfo info = BslSelectionHelper.resolve(currentSelection, targetPart).orElse(null);
        if (info == null || info.getFunctionName() == null || info.getModuleName() == null) {
            MessageDialog.openWarning(
                targetPart.getSite().getShell(),
                "Show Call Graph",
                "Could not determine function info"
            );
            return;
        }

        showCallGraph(info.getModuleName(), info.getFunctionName());
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        currentSelection = selection;
    }

    private void showCallGraph(String moduleName, String functionName) {
        BackendConnector backend = new BackendConnector();

        // Show progress
        MessageDialog progress = new MessageDialog(
            targetPart.getSite().getShell(),
            "Call Graph",
            null,
            "Building call graph for: " + functionName + "\nModule: " + moduleName,
            MessageDialog.INFORMATION,
            new String[] {"OK"},
            0
        );

        new Thread(() -> {
            try {
                // Get dependencies from Neo4j
                JsonObject result = backend.analyzeDependencies(moduleName, functionName);

                Display.getDefault().asyncExec(() -> {
                    if (result != null) {
                        // Show results
                        String message = formatCallGraph(functionName, result);
                        
                        MessageDialog.openInformation(
                            targetPart.getSite().getShell(),
                            "Call Graph: " + functionName,
                            message
                        );

                        // Open Metadata Graph view
                        try {
                            targetPart.getSite().getWorkbenchWindow().getActivePage()
                                .showView(MetadataGraphView.ID);
                        } catch (PartInitException e) {
                            // Ignore
                        }

                    } else {
                        MessageDialog.openError(
                            targetPart.getSite().getShell(),
                            "Error",
                            "Failed to build call graph. Check backend connection."
                        );
                    }
                });

            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openError(
                        targetPart.getSite().getShell(),
                        "Error",
                        "Call graph error: " + e.getMessage()
                    );
                });
            }
        }).start();
    }

    private String formatCallGraph(String functionName, JsonObject result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Граф вызовов для функции: ").append(functionName).append("\n\n");

        if (result.has("result")) {
            JsonObject data = result.getAsJsonObject("result");

            // Called by
            if (data.has("called_by")) {
                JsonArray calledBy = data.getAsJsonArray("called_by");
                sb.append("▲ Вызывается из (").append(calledBy.size()).append("):\n");
                
                for (int i = 0; i < calledBy.size() && i < 10; i++) {
                    JsonObject caller = calledBy.get(i).getAsJsonObject();
                    sb.append("  • ").append(caller.get("module").getAsString())
                      .append(".").append(caller.get("function").getAsString())
                      .append("\n");
                }
                
                if (calledBy.size() > 10) {
                    sb.append("  ... и еще ").append(calledBy.size() - 10).append("\n");
                }
                sb.append("\n");
            }

            // Calls to
            if (data.has("calls_to")) {
                JsonArray callsTo = data.getAsJsonArray("calls_to");
                sb.append("▼ Вызывает (").append(callsTo.size()).append("):\n");
                
                for (int i = 0; i < callsTo.size() && i < 10; i++) {
                    JsonObject callee = callsTo.get(i).getAsJsonObject();
                    sb.append("  • ").append(callee.get("module").getAsString())
                      .append(".").append(callee.get("function").getAsString())
                      .append("\n");
                }
                
                if (callsTo.size() > 10) {
                    sb.append("  ... и еще ").append(callsTo.size() - 10).append("\n");
                }
            }
        } else {
            sb.append("Результаты будут доступны после полной интеграции\n");
            sb.append("Backend response: ").append(result.toString());
        }

        return sb.toString();
    }
}







