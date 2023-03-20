const path = require('path')
const getWebpackConfig = require('@jetbrains/teamcity-api/getWebpackConfig')

module.exports = getWebpackConfig({
    srcPath: path.join(__dirname, './src'),
    outputPath: path.resolve(__dirname, './build/js'),
    entry: './src/main/js/index.js',
    useFlow: true,
})
