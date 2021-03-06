/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.brooklyn.ambari.rest.domain;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

public class Users {

    @SerializedName("user_name")
    private String username;

    @SerializedName("old_password")
    private String oldpassword;

    @SerializedName("password")
    private String password;

    public static class Builder {

        private String username;

        private String oldpassword;

        private String password;

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setOldPassword(String oldpassword) {
            this.oldpassword = oldpassword;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Users build() {
            Preconditions.checkNotNull(this.username);
            Preconditions.checkNotNull(this.oldpassword);

            Users users = new Users();
            users.username = this.username;
            users.oldpassword = this.oldpassword;
            users.password = this.password;

            return users;
        }
    }
}
