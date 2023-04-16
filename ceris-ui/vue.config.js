const path = require("path");

module.exports = {
    outputDir: path.resolve(__dirname, "../ceris-agent/src/main/resources/public"),
    transpileDependencies: [
        'vuetify', "@koumoul/vjsf"
    ]
}
