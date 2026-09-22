export interface ModuleConfig {
  key: string;
  title: string;
  subtitle: string;
  description: string;
  sprint: string;
  sprintBadgeClass: string;
  iconName: string;
  iconBgClass: string;
  iconTextClass: string;
  features: { title: string; desc: string }[];
  eta: string;
}
