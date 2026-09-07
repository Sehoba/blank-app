import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'de.seba.gorillaflamingo',
  appName: 'Gorilla & Fia',
  webDir: 'dist',
  server: { androidScheme: 'https' },
  android: { backgroundColor: '#07140d' },
};

export default config;
