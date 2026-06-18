const roleLabels: Record<string, string> = {
  evidence_analyst: 'Evidence analyst',
  skeptic: 'Skeptic',
  connector: 'Connector',
  empathy_reader: 'Empathy reader',
  style_reader: 'Style reader',
};

export function personaRoleLabel(roleKey?: string) {
  if (!roleKey) {
    return 'Persona';
  }
  return roleLabels[roleKey] || 'Persona';
}
