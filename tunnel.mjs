import localtunnel from 'localtunnel';

const PORT = 5173;
const SUBDOMAIN = 'pulsevent-live';

async function startTunnel() {
  try {
    console.log(`Starting tunnel on port ${PORT}...`);
    const tunnel = await localtunnel({
      port: PORT,
      subdomain: SUBDOMAIN,
      local_host: '127.0.0.1'
    });

    console.log(`\n========================================`);
    console.log(`  TUNNEL ONLINE: ${tunnel.url}`);
    console.log(`========================================\n`);

    tunnel.on('close', () => {
      console.log('Tunnel closed. Reconnecting in 3s...');
      setTimeout(startTunnel, 3000);
    });

    tunnel.on('error', (err) => {
      console.error('Tunnel error:', err.message);
      tunnel.close();
    });
  } catch (err) {
    console.error('Failed to start tunnel:', err.message);
    setTimeout(startTunnel, 5000);
  }
}

startTunnel();
