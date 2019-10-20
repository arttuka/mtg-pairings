module.exports = {
  entry: {
    'downshift': './src/js/downshift.js'
  },
  output: {
    filename: '[name].inc.js'
  },
  externals: {
    'react': 'React'
  }
};
