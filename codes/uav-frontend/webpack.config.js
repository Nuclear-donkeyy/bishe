const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const backendTarget = process.env.BACKEND_TARGET || 'http://localhost:8080';
const websocketTarget = backendTarget.replace(/^http/i, 'ws');

module.exports = {
  entry: path.resolve(__dirname, 'src/index.tsx'),
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: '[name].[contenthash].js',
    publicPath: '/',
    clean: true
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        use: 'ts-loader',
        exclude: /node_modules/
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader']
      },
      {
        test: /\.less$/i,
        use: [
          'style-loader',
          'css-loader',
          {
            loader: 'less-loader',
            options: {
              lessOptions: {
                javascriptEnabled: true
              }
            }
          }
        ]
      },
      {
        test: /\.(png|jpg|gif|svg|woff2?|ttf|eot)$/i,
        type: 'asset/resource'
      }
    ]
  },
  devServer: {
    static: {
      directory: path.join(__dirname, 'public')
    },
    historyApiFallback: true,
    hot: true,
    host: '0.0.0.0',
    port: 5173,
    proxy: [
      {
        context: ['/api'],
        target: backendTarget,
        changeOrigin: true
      },
      {
        context: ['/ws'],
        target: websocketTarget,
        ws: true,
        changeOrigin: true
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'public/index.html'),
      favicon: false
    })
  ]
};
