package org.ma3map.api.handlers;

import java.util.ArrayList;

import org.ma3map.api.listeners.ProgressListener;

public class ProgressHandler {
	
	private final ArrayList<ProgressListener> progressListeners;
    
    public ProgressHandler(){
    	progressListeners = new ArrayList<ProgressListener>();
    }
    
    /**
     * Adds a <code>ProgressListener</code> to the list of progress listeners that will be updated
     * when an action performed by this class is updated.
     * <p>
     * Note that not all actions performed by this class update registered progress listeners
     * <p>
     * @param progressListener  The new <code>ProgressListener</code> to be added to the list of updated
     *                          progress listeners
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    public void addProgressListener(final ProgressListener progressListener){
        progressListeners.add(progressListener);
    }

    /**
     * Updates all registered <code>ProgressListeners</code>
     * <p>
     * @param progress  Number not greater than <code>end</code> that indicates progress
     *                  <code>progress/end</code> should indicate progress as a fraction
     * @param end       Indicates the potential maximum value of <code>progress</code>
     * @param message   String indicating the progress. Can be displayed to the user
     * @param flag      Indicates the status of the task in question. Possible values are:
     *                      - {@link ProgressListener#FLAG_DONE}
     *                      - {@link ProgressListener#FLAG_WORKING}
     *                      - {@link ProgressListener#FLAG_ERROR}
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    protected void updateProgressListeners(int progress, int end, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onProgress(progress, end, message, flag);
        }
    }

    /**
     * Calls {@link ProgressListener#onDone(android.os.Bundle, String, int)} on all the registered
     * progress listeners.
     * <p>
     * @param output    <code>Bundle</code> containing all packaged data
     * @param message   String indicating finalisation of action. Can be displayed to the user
     * @param flag      Indicates the status of the task in question. Possible values are:
     *                      - {@link ProgressListener#FLAG_DONE}
     *                      - {@link ProgressListener#FLAG_WORKING}
     *                      - {@link ProgressListener#FLAG_ERROR}
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    protected void finalizeProgressListeners(Object output, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onDone(output, message, flag);
        }
    }
}
