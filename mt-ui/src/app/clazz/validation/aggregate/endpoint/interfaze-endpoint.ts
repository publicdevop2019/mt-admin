import { IIdBasedEntity } from "src/app/clazz/summary.component";

export interface IEndpoint extends IIdBasedEntity {
  resourceId: string;
  resourceName?: string;
  description?: string;
  name: string;
  path: string;
  method?: string;
  websocket: boolean;
  secured: boolean;
  shared: boolean;
  csrfEnabled: boolean;
  corsProfileId?: string;
  projectId?: string;
  cacheProfileId?: string;
  expired?: boolean;
  expireReason?: string;

}
export const HTTP_METHODS = [
  { label: 'HTTP_GET', value: "GET" },
  { label: 'HTTP_POST', value: "POST" },
  { label: 'HTTP_PUT', value: "PUT" },
  { label: 'HTTP_DELETE', value: "DELETE" },
  { label: 'HTTP_PATCH', value: "PATCH" },
]