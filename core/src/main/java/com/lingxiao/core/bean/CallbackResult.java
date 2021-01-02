package com.lingxiao.core.bean;

import java.util.List;

public class CallbackResult {

    /**
     * lastsave : 2021-01-02T06:13:56.516Z
     * changesurl : http://192.168.26.129:6831/cache/files/-2103450856_6197/changes.zip/changes.zip?md5=Jy0MH1n-Z0GO4QOvRtUtiQ&expires=1609568953&disposition=attachment&filename=changes.zip
     * history : {"serverVersion":"6.1.0","changes":[{"created":"2021-01-02 06:13:56","user":{"name":"John Smith","id":"78ele841"}}]}
     * forcesavetype : 1
     * key : -2103450856
     * url : http://192.168.26.129:6831/cache/files/-2103450856_6197/output.xlsx/output.xlsx?md5=ezZos-Gem04LPyq4s-hCng&expires=1609568953&disposition=attachment&filename=output.xlsx
     * users : ["78ele841"]
     * status : 6
     */

    private String lastsave;
    private String changesurl;
    private HistoryBean history;
    private int forcesavetype;
    private String key;
    private String url;
    private String token;
    private int status;
    private List<String> users;

    public String getLastsave() {
        return lastsave;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setLastsave(String lastsave) {
        this.lastsave = lastsave;
    }

    public String getChangesurl() {
        return changesurl;
    }

    public void setChangesurl(String changesurl) {
        this.changesurl = changesurl;
    }

    public HistoryBean getHistory() {
        return history;
    }

    public void setHistory(HistoryBean history) {
        this.history = history;
    }

    public int getForcesavetype() {
        return forcesavetype;
    }

    public void setForcesavetype(int forcesavetype) {
        this.forcesavetype = forcesavetype;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public static class HistoryBean {
        /**
         * serverVersion : 6.1.0
         * changes : [{"created":"2021-01-02 06:13:56","user":{"name":"John Smith","id":"78ele841"}}]
         */

        private String serverVersion;
        private List<ChangesBean> changes;

        public String getServerVersion() {
            return serverVersion;
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public List<ChangesBean> getChanges() {
            return changes;
        }

        public void setChanges(List<ChangesBean> changes) {
            this.changes = changes;
        }

        public static class ChangesBean {
            /**
             * created : 2021-01-02 06:13:56
             * user : {"name":"John Smith","id":"78ele841"}
             */

            private String created;
            private UserBean user;

            public String getCreated() {
                return created;
            }

            public void setCreated(String created) {
                this.created = created;
            }

            public UserBean getUser() {
                return user;
            }

            public void setUser(UserBean user) {
                this.user = user;
            }

            public static class UserBean {
                /**
                 * name : John Smith
                 * id : 78ele841
                 */

                private String name;
                private String id;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getId() {
                    return id;
                }

                public void setId(String id) {
                    this.id = id;
                }
            }
        }
    }
}
