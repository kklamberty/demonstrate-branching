import { Component, Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CompanyCardComponent } from '../company-card/company-card.component';
import { UserService } from '../users/user.service';
import { Company } from './company';

@Component({
  selector: 'app-company-list',
  imports: [CompanyCardComponent],
  templateUrl: './company-list.component.html',
  styleUrl: './company-list.component.scss'
})
export class CompanyListComponent {
  companies: Signal<Company[]> = toSignal(this.userService.getCompanies());

  constructor(private userService: UserService) {}
}
