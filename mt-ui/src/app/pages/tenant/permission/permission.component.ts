import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MatBottomSheetRef, MAT_BOTTOM_SHEET_DATA } from '@angular/material/bottom-sheet';
import { FormInfoService } from 'mt-form-builder';
import { IQueryProvider } from 'mt-form-builder/lib/classes/template.interface';
import { Aggregate } from 'src/app/clazz/abstract-aggregate';
import { IBottomSheet } from 'src/app/clazz/summary.component';
import { IEndpoint } from 'src/app/clazz/validation/aggregate/endpoint/interfaze-endpoint';
import { IPermission } from 'src/app/clazz/validation/aggregate/permission/interface-permission';
import { PermissionValidator } from 'src/app/clazz/validation/aggregate/permission/validator-permission';
import { ErrorMessage } from 'src/app/clazz/validation/validator-common';
import { FORM_CONFIG } from 'src/app/form-configs/permission.config';
import { HttpProxyService } from 'src/app/services/http-proxy.service';
import { MyEndpointService } from 'src/app/services/my-endpoint.service';
import { MyPermissionService } from 'src/app/services/my-permission.service';

@Component({
  selector: 'app-permission',
  templateUrl: './permission.component.html',
  styleUrls: ['./permission.component.css']
})
export class PermissionComponent extends Aggregate<PermissionComponent, IPermission> implements OnInit, OnDestroy {
  bottomSheet: IBottomSheet<IPermission>;
  constructor(
    public entityService: MyPermissionService,
    public epSvc: MyEndpointService,
    public httpProxySvc: HttpProxyService,
    fis: FormInfoService,
    @Inject(MAT_BOTTOM_SHEET_DATA) public data: any,
    bottomSheetRef: MatBottomSheetRef<PermissionComponent>,
    cdr: ChangeDetectorRef
  ) {
    super('permission-form', JSON.parse(JSON.stringify(FORM_CONFIG)), new PermissionValidator(), bottomSheetRef, data, fis, cdr)
    this.bottomSheet = data;
    this.entityService.setProjectId(this.bottomSheet.params['projectId']);

    this.epSvc.setProjectId(this.bottomSheet.params['projectId'])
    this.fis.queryProvider[this.formId + '_' + 'parentId'] = this.getParentPerm();
    this.fis.queryProvider[this.formId + '_' + 'apiId'] = this.getEndpoints();
    this.fis.formCreated(this.formId).subscribe(() => {
      if (this.bottomSheet.context === 'new') {
        this.fis.formGroupCollection[this.formId].get('projectId').setValue(this.bottomSheet.params['projectId'])
      }
      this.fis.formGroupCollection[this.formId].get('linkApi').valueChanges.subscribe(next=>{
        if(next){
          this.fis.showIfMatch(this.formId,['apiId'])
        }else{
          this.fis.hideIfMatch(this.formId,['apiId'])
        }
      })
      if(this.aggregate){
        if(this.aggregate.linkedApiId){
          this.fis.showIfMatch(this.formId,['apiId'])
        }

        this.fis.restore(this.formId,{
          id:this.aggregate.id,
          name:this.aggregate.name,
          parentId:this.aggregate.parentId,
          apiId:this.aggregate.linkedApiId,
          linkApi:!!this.aggregate.linkedApiId
        })
      }
    })
  }
  getParentPerm(): IQueryProvider {
    return {
      readByQuery: (num: number, size: number, query?: string, by?: string, order?: string, header?: {}) => {
        return this.httpProxySvc.readEntityByQuery<IPermission>(this.entityService.entityRepo, num, size, `types:COMMON.PROJECT`, by, order, header)
      }
    } as IQueryProvider
  }
  getEndpoints():IQueryProvider {
    return {
      readByQuery: (num: number, size: number, query?: string, by?: string, order?: string, header?: {}) => {
        return this.httpProxySvc.readEntityByQuery<IEndpoint>(this.epSvc.entityRepo, num, size, query, by, order, header)
      }
    } as IQueryProvider
  }
  ngOnDestroy(): void {
    this.fis.resetAllExcept(['summaryPermissionCustomerView'])
  }
  ngOnInit() {
  }
  convertToPayload(cmpt: PermissionComponent): IPermission {
    let formGroup = cmpt.fis.formGroupCollection[cmpt.formId];
    return {
      id: formGroup.get('id').value,//value is ignored
      parentId: formGroup.get('parentId').value,
      name: formGroup.get('name').value,
      projectId: formGroup.get('projectId').value,
      linkedApiId: formGroup.get('apiId').value,
      version: cmpt.aggregate && cmpt.aggregate.version
    }
  }
  update() {
    this.entityService.update(this.aggregate.id, this.convertToPayload(this), this.changeId)
  }
  create() {
    this.entityService.create(this.convertToPayload(this), this.changeId)
  }
  errorMapper(original: ErrorMessage[], cmpt: PermissionComponent) {
    return original.map(e => {
      return {
        ...e,
        formId: cmpt.formId
      }
    })
  }
}