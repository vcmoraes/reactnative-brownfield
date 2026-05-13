import * as Device from 'expo-device';
import { DeviceType } from 'expo-device';
import Constants from 'expo-constants';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

function deviceTypeLabel(type: DeviceType | null | undefined): string {
  if (type == null) return '—';
  switch (type) {
    case DeviceType.PHONE:
      return 'Telefone';
    case DeviceType.TABLET:
      return 'Tablet';
    case DeviceType.DESKTOP:
      return 'Desktop';
    case DeviceType.TV:
      return 'TV';
    case DeviceType.UNKNOWN:
    default:
      return 'Desconhecido';
  }
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <ThemedText type="defaultSemiBold" style={styles.label}>
        {label}
      </ThemedText>
      <ThemedText selectable style={styles.value}>
        {value}
      </ThemedText>
    </View>
  );
}

export default function DevicePocScreen() {
  const rows: { label: string; value: string }[] = [
    { label: 'Marca', value: Device.brand ?? '—' },
    { label: 'Fabricante', value: Device.manufacturer ?? '—' },
    { label: 'Modelo', value: Device.modelName ?? '—' },
    { label: 'Nome do dispositivo', value: Device.deviceName ?? '—' },
    { label: 'Sistema', value: Device.osName ?? '—' },
    { label: 'Versão do SO', value: Device.osVersion ?? '—' },
    { label: 'Tipo', value: deviceTypeLabel(Device.deviceType) },
    {
      label: 'Dispositivo físico',
      value: Device.isDevice ? 'Sim' : 'Não (ex.: simulador ou web)',
    },
    { label: 'Versão nativa do app', value: Constants.nativeApplicationVersion ?? '—' },
    { label: 'Build nativo', value: Constants.nativeBuildVersion ?? '—' },
  ];

  return (
    <ThemedView style={styles.screen}>
      <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled">
          <ThemedText type="title" style={styles.title}>
            POC — informações do dispositivo
          </ThemedText>
          <ThemedText style={styles.lead}>
            Dados via{' '}
            <ThemedText type="defaultSemiBold">expo-device</ThemedText> e{' '}
            <ThemedText type="defaultSemiBold">expo-constants</ThemedText>.
          </ThemedText>
          <View style={styles.card}>
            {rows.map((r) => (
              <Row key={r.label} label={r.label} value={r.value} />
            ))}
          </View>
        </ScrollView>
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  safe: {
    flex: 1,
  },
  scroll: {
    padding: 20,
    paddingBottom: 40,
  },
  title: {
    marginBottom: 8,
  },
  lead: {
    marginBottom: 20,
    opacity: 0.85,
  },
  card: {
    gap: 16,
  },
  row: {
    gap: 4,
    paddingVertical: 4,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(128,128,128,0.35)',
  },
  label: {
    fontSize: 13,
    opacity: 0.75,
  },
  value: {
    fontSize: 16,
  },
});
