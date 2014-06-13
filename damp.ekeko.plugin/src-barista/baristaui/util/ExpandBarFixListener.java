package baristaui.util;

import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;

public class ExpandBarFixListener implements ExpandListener {
	
	private ExpandBar bar;
	
	public ExpandBarFixListener(ExpandBar bar){
		this.bar = bar;
	}
	
	 private void resize(final ExpandEvent event, final boolean expand){

         final Display display = Display.getCurrent();

         new Thread(new Runnable() {
             public void run() {

                 final int[] orgSize = new int[1];
                 final int[] currentSize = new int[1];

                 final Object lock = new Object();

                 if (display.isDisposed() || bar.isDisposed()){
                     return;
                 }

                 display.syncExec(new Runnable() {
                     public void run() {
                         if (bar.isDisposed() || bar.getParent().isDisposed()){
                             return;
                         }

                         synchronized(lock){
                         	//config.getParent().pack(true);
                             orgSize[0] = bar.getSize().y;
                             currentSize[0] = orgSize[0];
                         }
                     }
                 });     

                 while (currentSize[0] == orgSize[0]){
                     if (display.isDisposed() || bar.isDisposed()){
                         return;
                     }
                     display.syncExec(new Runnable() {
                         public void run() {

                             synchronized(lock){
                                 if (bar.isDisposed() || bar.getParent().isDisposed()){
                                     return;
                                 }

                                 currentSize[0] = bar.getSize().y;

                                 if (currentSize[0] != orgSize[0]){
                                     return;
                                 }
                                 else{
                                 	bar.getParent().layout(true);
                                 	//config.getParent().pack(true);
                                 }
                             }
                         }
                     });
                 }
                 
             }
         }).start();
 }

 public void itemCollapsed(ExpandEvent event) {
     resize(event, false);
 }

 public void itemExpanded(ExpandEvent event) {        
     resize(event, true);
 }


}
