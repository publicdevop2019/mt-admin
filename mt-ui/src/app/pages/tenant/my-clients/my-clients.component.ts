import { Component, OnDestroy } from '@angular/core';
import { MatBottomSheet } from '@angular/material/bottom-sheet';
import { ActivatedRoute } from '@angular/router';
import { FormInfoService } from 'mt-form-builder';
import { IOption, ISumRep } from 'mt-form-builder/lib/classes/template.interface';
import { combineLatest } from 'rxjs';
import { take } from 'rxjs/operators';
import { CONST_GRANT_TYPE } from 'src/app/clazz/constants';
import { SummaryEntityComponent } from 'src/app/clazz/summary.component';
import { IClient } from 'src/app/clazz/validation/aggregate/client/interfaze-client';
import { ISearchConfig } from 'src/app/components/search/search.component';
import { ClientComponent } from 'src/app/pages/tenant/client/client.component';
import { DeviceService } from 'src/app/services/device.service';
import { MyClientService } from 'src/app/services/my-client.service';
import { ProjectService } from 'src/app/services/project.service';

@Component({
  selector: 'app-my-clients',
  templateUrl: './my-clients.component.html',
  styleUrls: ['./my-clients.component.css']
})
export class MyClientsComponent extends SummaryEntityComponent<IClient, IClient> implements OnDestroy{
  public formId = "myClientTableColumnConfig";
  columnList = {
    name: 'NAME',
    id: 'ID',
    description: 'DESCRIPTION',
    resourceIndicator: 'RESOURCE_INDICATOR',
    grantTypeEnums: 'GRANTTYPE_ENUMS',
    types: 'TYPES',
    accessTokenValiditySeconds: 'ACCESS_TOKEN_VALIDITY_SECONDS',
    resourceIds: 'RESOURCEIDS',
    edit: 'EDIT',
    delete: 'DELETE',
  }
  sheetComponent = ClientComponent;
  public projectId: string;
  public grantTypeList: IOption[] = CONST_GRANT_TYPE;
  resourceClientList: IOption[] = [];
  searchConfigs: ISearchConfig[] = [
  ]
  private initSearchConfigs: ISearchConfig[] = [
    {
      searchLabel: 'ID',
      searchValue: 'id',
      type: 'text',
      multiple: {
        delimiter: '.'
      }
    },
    {
      searchLabel: 'NAME',
      searchValue: 'name',
      type: 'text',
      multiple: {
        delimiter: '.'
      }
    },
    {
      searchLabel: 'GRANTTYPE_ENUMS',
      searchValue: 'grantTypeEnums',
      type: 'dropdown',
      multiple: {
        delimiter: '$'
      },
      source: CONST_GRANT_TYPE
    },
    {
      searchLabel: 'RESOURCE_INDICATOR',
      searchValue: 'resourceIndicator',
      type: 'boolean',
    },
    {
      searchLabel: 'ACCESS_TOKEN_VALIDITY_SECONDS',
      searchValue: 'accessTokenValiditySeconds',
      type: 'range',
    },
  ]
  constructor(
    public entitySvc: MyClientService,
    public projectSvc: ProjectService,
    public fis: FormInfoService,
    public deviceSvc: DeviceService,
    public bottomSheet: MatBottomSheet,
    private route: ActivatedRoute,
  ) {
    super(entitySvc, deviceSvc, bottomSheet,fis, 3);
    this.route.paramMap.pipe(take(1)).subscribe(queryMaps => {
      this.projectId = queryMaps.get('id')
      this.entitySvc.setProjectId(this.projectId)
      this.bottomSheetParams['projectId']=this.projectId;
    });
    combineLatest([this.entitySvc.readEntityByQuery(0, 1000, 'resourceIndicator:1')]).pipe(take(1))//@todo use paginated select component
      .subscribe(next => {
        if (next) {
          this.searchConfigs = [...this.initSearchConfigs, {
            searchLabel: 'RESOURCEIDS',
            searchValue: 'resourceIds',
            type: 'dropdown',
            multiple: {
              delimiter: '.'
            },
            source: next[0].data.map(e => {
              return {
                label: e.name,
                value: e.id
              }
            })
          },
          ];
        }
      });
  }
  updateSummaryData(next: ISumRep<IClient>) {
    super.updateSummaryData(next);
    let var0 = new Set(next.data.flatMap(e => e.resourceIds).filter(ee => ee));
    let var1 = new Array(...var0);
    if (var1.length > 0) {
      this.entitySvc.readEntityByQuery(0, var1.length, "clientId:" + var1.join('.')).subscribe(next => {
        this.resourceClientList = next.data.map(e => <IOption>{ label: e.name, value: e.id });
      })
    }
  }
  getList(inputs: string[]) {
    return inputs.map(e => <IOption>{ label: e, value: e })
  }
  getResourceList(inputs?: string[]) {
    return this.resourceClientList.filter(e => inputs?.includes(e.value + ''))
  }
  removeFirst(input: string[]) {
    return input.filter((e, i) => i !== 0);
  }
}