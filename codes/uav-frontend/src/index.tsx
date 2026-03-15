import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import App from './App';
import 'antd/dist/reset.css';
import './styles/global.css';
import 'leaflet/dist/leaflet.css';
import { AuthProvider } from './context/AuthContext';

const container = document.getElementById('root');

if (!container) {
  throw new Error('Root container not found');
}

const root = createRoot(container);

root.render(
  <React.StrictMode>
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1565f5',
          colorInfo: '#1565f5',
          colorSuccess: '#0f9f8f',
          colorWarning: '#d9961a',
          colorError: '#d14343',
          borderRadius: 12,
          borderRadiusLG: 16,
          fontFamily:
            "'Manrope', 'Noto Sans SC', 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif",
          colorBgLayout: '#eff4fb',
          colorBgContainer: 'rgba(255,255,255,0.82)',
          colorBorderSecondary: 'rgba(139, 163, 196, 0.22)',
          boxShadowSecondary: '0 18px 48px rgba(26, 73, 140, 0.10)'
        },
        components: {
          Layout: {
            siderBg: '#081a38',
            triggerBg: '#0c244c',
            triggerColor: '#dce8ff',
            headerBg: 'rgba(255,255,255,0.78)',
            bodyBg: '#eff4fb'
          },
          Menu: {
            darkItemBg: 'transparent',
            darkSubMenuItemBg: 'transparent',
            darkItemColor: 'rgba(222,232,255,0.76)',
            darkItemSelectedBg: 'rgba(255,255,255,0.14)',
            darkItemSelectedColor: '#ffffff',
            darkItemHoverBg: 'rgba(255,255,255,0.08)'
          },
          Card: {
            colorBorderSecondary: 'rgba(139, 163, 196, 0.18)',
            headerFontSize: 16
          },
          Button: {
            primaryShadow: '0 10px 24px rgba(21, 101, 245, 0.24)',
            borderRadius: 10
          },
          Input: {
            activeBorderColor: '#1565f5',
            hoverBorderColor: '#7ba8ff'
          },
          Select: {
            optionSelectedBg: 'rgba(21, 101, 245, 0.08)'
          },
          Table: {
            headerBg: '#f4f8fe',
            rowHoverBg: '#f7fbff'
          }
        }
      }}
    >
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>
);
