<template>
  <v-app>
    <v-main>
      <v-container fluid fill-height>
        <v-layout align-center justify-center>
          <v-flex xs12 sm8 md4>
            <v-card class="elevation-12">
              <v-toolbar dark color="primary">
                <v-toolbar-title>Ceris Login</v-toolbar-title>
              </v-toolbar>
              <v-card-text>
                <v-form ref="form" validate-on="input" v-model="isFormValid" @submit.prevent="submit()">
                  <v-text-field
                      label="User"
                      v-model="username"
                      :rules="[v => !!v || 'Username is required']"
                      required
                  ></v-text-field>
                  <v-text-field
                      label="Password"
                      v-model="password"
                      :rules="[v => !!v || 'Password is required']"
                      type="password"
                      required
                  ></v-text-field>
                  <v-btn :disabled="!isFormValid" type="submit" class="mt-4" color="primary">Login</v-btn>
                </v-form>
              </v-card-text>
            </v-card>
            <v-progress-linear v-if="loading" indeterminate color="blue"/>
            <div class="pa-3"/>
            <v-alert v-if="errorMsg" dense light border="top" outlined type="error">{{ errorMsg }}</v-alert>
          </v-flex>
        </v-layout>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>

import {mapActions} from "vuex";

export default {
  name: "Login",
  data() {
    return {
      username: "",
      password: "",
      errorMsg: "",
      loading: false,
      isFormValid: false,
    };
  },
  methods: {
    ...mapActions(['login']),
    submit() {
      if (this.$refs.form.validate()) {
        this.loading = true
        this.login({username: this.username, password: this.password})
            .then(() => this.$router.push('/'))
            .catch(e => this.errorMsg = e.respnse ? e.response.data.message : e.message)
            .finally(() => this.loading = false)
      }
    }
  }
}

</script>