package com.lingxiao.core;

/**
 * @author lingxiao
 */
public enum DocumentSaveStatus{
        //没有发现这个文档
        NOT_FOUNT_DOCUMENT(0,"no document with the key identifier could be found"),
        DOCUMENT_IS_EDITED(1,"document is being edited"),
        DOCUMENT_IS_SAVING(2,"document is ready for saving"),
        DOCUMENT_SAVING_FAILED(3,"document saving error has occurred"),
        DOCUMENT_IS_CLOSED_WITH_NO_CHANGES(4,"document is closed with no changes"),
        DOCUMENT_IS_SAVED(6,"document is being edited, but the current document state is saved"),
        DOCUMENT_FORCE_SAVING_FAILED(7,"error has occurred while force saving the document"),
        ;
        private int status;
        private String message;
        DocumentSaveStatus(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
                return status;
        }

        public String getMessage() {
                return message;
        }
}